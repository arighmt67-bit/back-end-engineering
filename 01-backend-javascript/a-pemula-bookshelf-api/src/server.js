const Hapi = require('@hapi/hapi');
const routes = require('./routes');

const init = async () => {
  const port = process.env.PORT || 9000;
  const host = process.env.NODE_ENV === 'production' ? '0.0.0.0' : 'localhost';

  const server = Hapi.server({
    port,
    host,
    routes: {
      cors: {
        origin: ['*'],
      },
    },
  });

  server.route(routes);

  await server.start();
  console.log(`Server berjalan pada ${server.info.uri}`);
};

init();
