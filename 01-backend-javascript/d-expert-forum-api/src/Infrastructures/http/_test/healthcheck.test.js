import request from 'supertest';
import container from '../../container.js';
import createServer from '../createServer.js';
import pool from '../../database/postgres/pool.js';

/**
 * Memastikan server membalas 404 untuk route yang tidak terdaftar.
 */
describe('Health check endpoint', () => {
  let server;

  beforeAll(async () => {
    server = await createServer(container);
  });

  afterAll(async () => {
    await pool.end();
  });

  it('should response 200 and status success when accessing root path', async () => {
    const response = await request(server).get('/');

    expect(response.status).toEqual(200);
    expect(response.body.status).toEqual('success');
    expect(response.body.message).toEqual('Forum API is running');
  });

  it('should response 404 for unregistered route', async () => {
    const response = await request(server).get('/unregistered-route');

    expect(response.status).toEqual(404);
    expect(response.body.status).toEqual('fail');
  });
});
