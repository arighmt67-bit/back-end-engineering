const { postPredictHandler, getHistoriesHandler } = require('./handler');

const routes = [
  {
    path: '/predict',
    method: 'POST',
    handler: postPredictHandler,
    options: {
      payload: {
        allow: 'multipart/form-data',
        multipart: true,
        maxBytes: 1000000,
        output: 'file',
        parse: true,
      },
    },
  },
  {
    path: '/predict/histories',
    method: 'GET',
    handler: getHistoriesHandler,
  },
  {
    path: '/',
    method: 'GET',
    handler: (request, h) => {
      return { message: 'Asclepius ML API is running.' };
    },
  },
];

module.exports = routes;
