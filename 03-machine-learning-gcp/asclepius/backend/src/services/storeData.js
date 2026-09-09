const { Firestore } = require('@google-cloud/firestore');

let firestoreInstance = null;
const localCache = new Map();

function getFirestore() {
  if (!firestoreInstance && process.env.NODE_ENV !== 'test') {
    try {
      firestoreInstance = new Firestore({
        projectId: process.env.PROJECT_ID,
        databaseId: process.env.FIRESTORE_DATABASE_ID || '(default)',
      });
    } catch (err) {
      console.warn('Firestore initialization warning:', err.message);
    }
  }
  return firestoreInstance;
}

async function storeData(id, data) {
  localCache.set(id, data);
  const db = getFirestore();
  if (db) {
    try {
      const predictCollection = db.collection('predictions');
      return await predictCollection.doc(id).set(data);
    } catch (e) {
      console.error('Failed storing to Firestore:', e.message);
    }
  }
}

async function getDataHistories() {
  const db = getFirestore();
  if (db) {
    try {
      const predictCollection = db.collection('predictions');
      const snapshot = await predictCollection.get();

      const histories = [];
      snapshot.forEach((doc) => {
        const docData = doc.data();
        histories.push({
          id: doc.id,
          history: {
            result: docData.result,
            createdAt: docData.createdAt,
            suggestion: docData.suggestion,
            id: docData.id || doc.id,
          },
        });
      });

      return histories;
    } catch (e) {
      console.error('Failed reading from Firestore, falling back to cache:', e.message);
    }
  }

  // Fallback to local cache
  const histories = [];
  localCache.forEach((docData, id) => {
    histories.push({
      id,
      history: {
        result: docData.result,
        createdAt: docData.createdAt,
        suggestion: docData.suggestion,
        id: docData.id || id,
      },
    });
  });
  return histories;
}

module.exports = { storeData, getDataHistories };
