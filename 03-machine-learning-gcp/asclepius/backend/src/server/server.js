require('dotenv').config();
const Hapi = require('@hapi/hapi');
const routes = require('./routes');
const loadModel = require('../services/loadModel');
const ClientError = require('../exceptions/ClientError');
const fs = require('fs');

const init = async () => {
  const port = process.env.PORT || 8080;
  const host = process.env.HOST || '0.0.0.0';

  const server = Hapi.server({
    port,
    host,
    routes: {
      cors: {
        origin: ['*'],
      },
    },
  });

  // Load ML Model
  console.log('Loading TensorFlow.js model...');
  const model = await loadModel();
  server.app.model = model;
  console.log('Model loaded successfully!');

  // Set file payload handling helper
  server.route(routes.map((r) => {
    if (r.path === '/predict' && r.method === 'POST') {
      return {
        ...r,
        options: {
          ...r.options,
          payload: {
            ...r.options.payload,
            output: 'data', // read to buffer directly
          },
        },
      };
    }
    return r;
  }));

  server.ext('onPreResponse', (request, h) => {
    const { response } = request;

    if (response instanceof Error) {
      // 1. Check if payload length > 1,000,000 bytes (Hapi 413)
      if (response.isBoom && response.output.statusCode === 413) {
        const newResponse = h.response({
          status: 'fail',
          message: 'Payload content length greater than maximum allowed: 1000000',
        });
        newResponse.code(413);
        return newResponse;
      }

      // 2. Custom ClientError (InputError)
      if (response instanceof ClientError) {
        const newResponse = h.response({
          status: 'fail',
          message: response.message,
        });
        newResponse.code(response.statusCode);
        return newResponse;
      }

      // 3. Any 400 bad request error from Hapi multipart parsing
      if (response.isBoom && response.output.statusCode === 400) {
        const newResponse = h.response({
          status: 'fail',
          message: 'Terjadi kesalahan dalam melakukan prediksi',
        });
        newResponse.code(400);
        return newResponse;
      }

      // 4. Default 500 error or others
      const newResponse = h.response({
        status: 'fail',
        message: response.message || 'Terjadi kesalahan pada server kami',
      });
      newResponse.code(response.isBoom ? response.output.statusCode : 500);
      return newResponse;
    }

    return h.continue;
  });

  await server.start();
  console.log(`Server running at: ${server.info.uri}`);
};

init().catch((err) => {
  console.error('Fatal Server Error:', err);
  process.exit(1);
});
