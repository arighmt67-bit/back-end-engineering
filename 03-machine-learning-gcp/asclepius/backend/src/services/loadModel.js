const tf = require('@tensorflow/tfjs');

async function loadModel() {
  const modelUrl = process.env.MODEL_URL;
  if (!modelUrl) {
    throw new Error('MODEL_URL environment variable is not defined');
  }
  return tf.loadGraphModel(modelUrl);
}

module.exports = loadModel;
