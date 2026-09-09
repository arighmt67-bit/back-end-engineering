const { v4: uuidv4 } = require('uuid');
const predictClassification = require('../services/inferenceService');
const { storeData, getDataHistories } = require('../services/storeData');
const InputError = require('../exceptions/InputError');

async function postPredictHandler(request, h) {
  const { image } = request.payload || {};
  const { model } = request.server.app;

  if (!image) {
    throw new InputError('Terjadi kesalahan dalam melakukan prediksi');
  }

  // Extract buffer from payload
  let buffer;
  if (Buffer.isBuffer(image)) {
    buffer = image;
  } else if (image._data && Buffer.isBuffer(image._data)) {
    buffer = image._data;
  } else {
    throw new InputError('Terjadi kesalahan dalam melakukan prediksi');
  }

  const { result, suggestion } = await predictClassification(model, buffer);

  const id = uuidv4();
  const createdAt = new Date().toISOString();

  const data = {
    id,
    result,
    suggestion,
    createdAt,
  };

  // Store into Firestore
  await storeData(id, data);

  const response = h.response({
    status: 'success',
    message: 'Model is predicted successfully',
    data,
  });
  response.code(201);
  return response;
}

async function getHistoriesHandler(request, h) {
  const histories = await getDataHistories();

  return h.response({
    status: 'success',
    data: histories,
  }).code(200);
}

module.exports = { postPredictHandler, getHistoriesHandler };
