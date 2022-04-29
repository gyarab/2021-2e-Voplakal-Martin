const express = require('express')
const app = express()
const Joi = require("joi");
const { ObjectId } = require('mongodb');
const port = 3000
const { dbUrl, dbPsw, dbParm } = require("./config.json")

var MongoClient = require('mongodb').MongoClient;
let DB;
MongoClient.connect(dbUrl + dbPsw + dbParm, function (err, db) {
  if (err) throw err;
  DB = db
  console.log("Connected!");
});

app.use(express.json({ limit: '10MB' }));
app.use("/static", express.static("static"));

/**
 * validace requiestů
 */
const schema = Joi.object({
  clientid: Joi.string().required()
})
const schemaRecept = Joi.object({
  nazev: Joi.string().min(3).max(100).required(),
  ingredience: Joi.array().min(1).required().items(Joi.string()),
  postup: Joi.string().required(),
  images: Joi.array().max(5).items(Joi.string())
});
const schemaBackup = Joi.object({
  nazev: Joi.string().min(3).max(100).required(),
  ingredience: Joi.array().min(0).items(Joi.string().allow('')),
  postup: Joi.string().allow(''),
  images: Joi.array().items(Joi.string().allow('')),
  clientid: Joi.string().required(),
  id: Joi.number().required()
});
/**
 * Validuje recept pro sdílení
 * @param r req.body
 * @returns 
 */
function validateRecept(r) {
  return schemaRecept.validate(r);
}
/**
 * validovat postkytlý request pro zálohování
 * @param  m request.body
 * @returns 
 */
function validateBackup(m) {
  return schemaBackup.validate(m);
}
//načtení templatu
const template = require("./template/template.html")
function render(o) {
  let arr = o.ingredience
  o.ingredience = ""
  for (let i = 0; i < arr.length; i++) {
    o.ingredience += "<li>" + arr[i] + "</li>";
  }
  o.htmlImages = "";
  if(!o.images) o.images = [];
  for (let i = 0; i < o.images.length; i++) {
    o.htmlImages += `<a href="" data-lightbox="image-1" data-title="${o.nazev}" >
    <img src="data:image/jpg;base64,${o.images[i]}" alt="${o.nazev}" />
  </a>`
  }
  if (o.htmlImages.trim() === "") o.htmlImages = '<p style="font-size: 0.9em; color: dimgray;">Žádné obrázky<br>nebyly přidány</p>'
  return template(o)
}

/**
 * všechny druhy requestů kteŕe aplikace používá...
 */


app.get('/shared', (req, res) => {
  // v promene req.query.param1 je "value1"
  DB.db("recepty").collection("shared").findOne({ _id: ObjectId(req.query.id) }, { projection: { _id: 0 } }, (dberr, dbres) => {
    if (dberr) {
      res.status(500).send()
      throw err;
    }
    if (req.query.json == "true") {
      res.send(JSON.stringify(dbres))
      return;
    }

    res.send(render(dbres))
  })
});


app.post("/share", (req, res) => {
  console.log("share...");
  const { error } = validateRecept(req.body);
  if (error) {
    res.status(400).send(error.details[0].message);
    console.log(error.details);
    return;
  } else {
    const recept = req.body;
    DB.db("recepty").collection("shared").insertOne(recept, function (dberr, dbres) {
      if (dberr) {
        res.status(500).send()
        throw dberr;
        return;
      }
      console.log("Inserted:");
      res.send(dbres.insertedId)
    });
  }
})

//backup
app.post("/backup", (req, res) => {
  console.log("backup");
  const { error } = validateBackup(req.body);
  if (error) {
    res.status(400).send(error.details[0].message);
    console.log(error);
    throw error;
    return;
  } else {
    const recept = req.body;
    DB.db("backup").collection(recept.clientid).insertOne(recept, function (dberr, dbres) {
      if (dberr) {
        res.status(500).send()
        throw dberr;
        return;
      }
      res.send("Inserted:" + dbres.insertedId)
    });
  }
})

app.get("/backup:get", (req, res) => {
  switch (req.params.get) {
    case "Code":
      DB.db("recepty").collection("clients").insertOne({ clientip: req.headers['x-forwarded-for'] }, function (dberr, dbres) {
        if (dberr) {
          res.status(500).send()
          throw dberr;
        }
        console.log("Inserted client: " + dbres.insertedId);
        res.send(dbres.insertedId)
      });
      break;
    case "Recept":
      if (!req.headers.clientkey || !req.headers.clientkey) {
        res.status(400).send(error.details[0].message);
        console.log(error.details);
        return;
      }
      console.log(req.headers.clientkey);
      DB.db("backup").collection(req.headers.clientkey).findOne({ _id: ObjectId(req.headers.id) }, { projection: { _id: 0 } }, (dberr, dbres) => {
        if (dberr) {
          res.status(500).send()
          throw err;
          return;
        }
        res.send(JSON.stringify(dbres))
      })
      break;
    //vrátí  [{_id: xxx}] receptů pro následný get z app
    default:
      res.status(400).send("Invalid argument");
      break;
  }
})

app.post("/backup:post", (req, res) => {
  switch (req.params.post) {
    //získat Ids pro následné stáhnutí každého jednotlivého receptu pomocí zde poskytlého ID
    case "IDs": {
      const { error } = schema.validate(req.body)
      if (error) {
        res.status(400).send(error.details[0].message);
        console.log(error.details);
        return;
      }
      console.log(req.body.clientid);
      DB.db("backup").collection(req.body.clientid).find({}, { projection: { _id: 1 } }).toArray(function (dberr, dbres) {
        if (dberr) {
          res.status(500).send()
          throw err;
        }
        res.send(dbres)
      });
    }
    break;
    //smazat starou zálohu
    case "Reset": {
      const { error } = schema.validate(req.body)
      if (error) {
        res.status(400).send(error.details[0].message);
        console.log(error.details);
        return;
      }
      DB.db("backup").collection(req.body.clientid).drop(function (err, delOK) {
        if (delOK) res.send("Collection deleted");
        else res.send("Nothing to delete")
      })
    }
      break;
    default:
      res.status(400).send("Invalid argument");
      break;
  }
});


app.listen(port, () => {
  console.log(`Recepty server listening on port ${port}`)
})

