const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// Region für alle Funktionen
const reg = "us-central1";

exports.biersync = functions.region(reg).https.onRequest(async (req: any, res: any) => {
    res.set("Access-Control-Allow-Origin", "*");
    if (req.method === "OPTIONS") return res.status(204).send("");
    try {
        const { token, uuid, name, bedrock } = req.body || {};
        const tDoc = await db.collection("sync_tokens").doc(token).get();
        if (!tDoc.exists) return res.status(400).send("No");
        const uid = tDoc.data().uid;
        await db.collection("users").doc(uid).set({ minecraftUuid: uuid, minecraftName: name, isBedrock: !!bedrock }, { merge: true });
        await tDoc.ref.delete();
        res.json({ success: true });
    } catch (e) { res.status(500).send("Error"); }
});

// Universeller Push-Trigger
const collections = ["tickets", "private_chats", "public_chat", "events", "forum", "market"];
collections.forEach(col => {
    exports[`on${col}`] = functions.region(reg).firestore.document(`${col}/{id}`).onCreate(async (snap: any) => {
        // Hier kommt die Push-Logik rein, sobald das Deployment einmal läuft!
        console.log("Neu in " + col);
        return null;
    });
});
