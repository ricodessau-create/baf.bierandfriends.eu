const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// Region für alle Funktionen
const reg = "us-central1";

// 1. Biersync Funktion (dein Original-Code)
exports.biersync = functions.region(reg).https.onRequest(async (req, res) => {
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

// 2. Automatischer Push-Trigger für alle wichtigen Collections
const collections = ["tickets", "private_chats", "public_chat", "events", "forum", "market", "private_messages"];

collections.forEach(col => {
    const funcName = `onNew${col.charAt(0).toUpperCase() + col.slice(1)}`;
    
    exports[funcName] = functions.region(reg).firestore.document(`${col}/{id}`).onCreate(async (snap) => {
        const newData = snap.data();
        
        // Wir brauchen eine ID, um zu wissen, wer die Push bekommen soll
        // In private_messages sollte das Feld 'receiverId' heißen
        const receiverId = newData.receiverId;

        if (!receiverId) {
            console.log(`Keine receiverId in Collection ${col} gefunden.`);
            return null;
        }

        try {
            // Hol den fcmToken des Empfängers aus der users-Collection
            const userDoc = await db.collection("users").doc(receiverId).get();
            const userData = userDoc.data();
            const fcmToken = userData ? userData.fcmToken : null;

            if (fcmToken) {
                const payload = {
                    notification: {
                        title: `Neue Nachricht`,
                        body: newData.text || `Du hast eine neue Benachrichtigung in ${col}.`,
                        sound: "default"
                    }
                };

                await admin.messaging().sendToDevice(fcmToken, payload);
                console.log(`Push-Benachrichtigung erfolgreich an ${receiverId} gesendet.`);
            } else {
                console.log(`User ${receiverId} hat keinen fcmToken hinterlegt.`);
            }
        } catch (error) {
            console.error("Fehler beim Senden der Push:", error);
        }
        return null;
    });
});
