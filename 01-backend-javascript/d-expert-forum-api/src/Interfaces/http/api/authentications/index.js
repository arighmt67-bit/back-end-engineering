import AuthenticationsHandler from './handler.js';
import createAuthenticationsRouter from './routes.js';

export default (container) => {
  const authenticationsHandler = new AuthenticationsHandler(container);
  return createAuthenticationsRouter(authenticationsHandler);
};
