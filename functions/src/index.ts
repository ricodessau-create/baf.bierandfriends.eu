import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

// Region für alle Funktionen
const reg = "us-central1";

export const biersync = functions.region(reg).https.onRequest(async (req, res) => {
    res.set("Access-Control-Allow-Origin", "*");
    if (req.method === "OPTIONS") {
        res.status(204).send("");
        return;
    }
    try {
        const { token, uuid, name, bedrock } = req.body || {};
        const tDoc = await db.collection("sync_tokens").doc(token).get();
        
        if (!tDoc.exists) {
            res.status(400).send("No");
            return;
        }

        const data = tDoc.data();
        const uid = data ? data.uid : null;

        if (uid) {
            await db.collection("users").doc(uid).set({ 
                minecraftUuid: uuid, 
                minecraftName: name, 
                isBedrock: !!bedrock 
            }, { merge: true });
            await tDoc.ref.delete();
        }
        
        res.json({ success: true });
    } catch (e) { 
        res.status(500).send("Error"); 
    }
});

// Universeller Push-Trigger
const collections = ["tickets", "private_chats", "public_chat", "events", "forum", "market"];
collections.forEach(col => {
    const funcName = `onNew${col.charAt(0).toUpperCase() + col.slice(1)}`;
    (exports as any)[funcName] = functions.region(reg).firestore.document(`${col}/{id}`).onCreate(async (snap) => {
        console.log("Neu in " + col);
        return null;
    });
});
