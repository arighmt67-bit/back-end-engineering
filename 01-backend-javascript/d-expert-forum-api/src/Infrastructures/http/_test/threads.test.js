import request from 'supertest';
import ThreadsTableTestHelper from '../../../../tests/ThreadsTableTestHelper.js';
import CommentsTableTestHelper from '../../../../tests/CommentsTableTestHelper.js';
import RepliesTableTestHelper from '../../../../tests/RepliesTableTestHelper.js';
import UsersTableTestHelper from '../../../../tests/UsersTableTestHelper.js';
import AuthenticationsTableTestHelper from '../../../../tests/AuthenticationsTableTestHelper.js';
import ServerTestHelper from '../../../../tests/ServerTestHelper.js';
import container from '../../container.js';
import createServer from '../createServer.js';
import pool from '../../database/postgres/pool.js';

describe('/threads endpoint', () => {
  let server;

  beforeAll(async () => {
    server = await createServer(container);
  });

  afterEach(async () => {
    await RepliesTableTestHelper.cleanTable();
    await CommentsTableTestHelper.cleanTable();
    await ThreadsTableTestHelper.cleanTable();
    await UsersTableTestHelper.cleanTable();
    await AuthenticationsTableTestHelper.cleanTable();
  });

  afterAll(async () => {
    await pool.end();
  });

  describe('when POST /threads', () => {
    it('should response 201 and persisted thread', async () => {
      // Arrange
      await UsersTableTestHelper.addUser({ id: 'user-123', username: 'dicoding' });
      const accessToken = ServerTestHelper.getAccessToken({ id: 'user-123', username: 'dicoding' });

      // Action
      const response = await request(server)
        .post('/threads')
        .set('Authorization', `Bearer ${accessToken}`)
        .send({ title: 'sebuah thread', body: 'sebuah body thread' });

      // Assert
      expect(response.status).toEqual(201);
      expect(response.body.status).toEqual('success');
      expect(response.body.data.addedThread).toBeDefined();
      expect(response.body.data.addedThread.title).toEqual('sebuah thread');
      expect(response.body.data.addedThread.owner).toEqual('user-123');
    });

    it('should response 400 when request payload not contain needed property', async () => {
      await UsersTableTestHelper.addUser({ id: 'user-123', username: 'dicoding' });
      const accessToken = ServerTestHelper.getAccessToken({ id: 'user-123', username: 'dicoding' });

      const response = await request(server)
        .post('/threads')
        .set('Authorization', `Bearer ${accessToken}`)
        .send({ title: 'sebuah thread' });

      expect(response.status).toEqual(400);
      expect(response.body.status).toEqual('fail');
      expect(response.body.message).toBeDefined();
    });

    it('should response 400 when request payload not meet data type specification', async () => {
      await UsersTableTestHelper.addUser({ id: 'user-123', username: 'dicoding' });
      const accessToken = ServerTestHelper.getAccessToken({ id: 'user-123', username: 'dicoding' });

      const response = await request(server)
        .post('/threads')
        .set('Authorization', `Bearer ${accessToken}`)
        .send({ title: 123, body: true });

      expect(response.status).toEqual(400);
      expect(response.body.status).toEqual('fail');
      expect(response.body.message).toBeDefined();
    });

    it('should response 401 when request not contain access token', async () => {
      const response = await request(server)
        .post('/threads')
        .send({ title: 'sebuah thread', body: 'sebuah body thread' });

      expect(response.status).toEqual(401);
    });

    it('should response 401 when access token is invalid', async () => {
      const response = await request(server)
        .post('/threads')
        .set('Authorization', 'Bearer invalid.access.token')
        .send({ title: 'sebuah thread', body: 'sebuah body thread' });

      expect(response.status).toEqual(401);
    });
  });

  describe('when GET /threads/{threadId}', () => {
    it('should response 200 and thread detail with comments and replies', async () => {
      // Arrange
      await UsersTableTestHelper.addUser({ id: 'user-123', username: 'dicoding' });
      await UsersTableTestHelper.addUser({ id: 'user-456', username: 'johndoe' });
      await ThreadsTableTestHelper.addThread({ id: 'thread-123', owner: 'user-123' });
      await CommentsTableTestHelper.addComment({
        id: 'comment-123', threadId: 'thread-123', owner: 'user-456', date: '2021-08-08T07:22:33.555Z',
      });
      await CommentsTableTestHelper.addComment({
        id: 'comment-456', threadId: 'thread-123', owner: 'user-123', date: '2021-08-08T07:26:21.338Z', isDelete: true,
      });
      await RepliesTableTestHelper.addReply({
        id: 'reply-123', commentId: 'comment-123', owner: 'user-123', isDelete: true,
      });

      // Action
      const response = await request(server).get('/threads/thread-123');

      // Assert
      expect(response.status).toEqual(200);
      expect(response.body.status).toEqual('success');

      const { thread } = response.body.data;
      expect(thread.id).toEqual('thread-123');
      expect(thread.username).toEqual('dicoding');
      expect(thread.comments).toHaveLength(2);
      expect(thread.comments[0].content).toEqual('sebuah comment');
      expect(thread.comments[0].replies).toHaveLength(1);
      expect(thread.comments[0].replies[0].content).toEqual('**balasan telah dihapus**');
      expect(thread.comments[1].content).toEqual('**komentar telah dihapus**');
    });

    it('should response 404 when thread not found', async () => {
      const response = await request(server).get('/threads/thread-xxx');

      expect(response.status).toEqual(404);
      expect(response.body.status).toEqual('fail');
      expect(response.body.message).toBeDefined();
    });
  });
});
