/* eslint-disable camelcase -- properti mengikuti nama kolom pada basis data */
import CommentLikesTableTestHelper from '../../../../tests/CommentLikesTableTestHelper.js';
import CommentsTableTestHelper from '../../../../tests/CommentsTableTestHelper.js';
import ThreadsTableTestHelper from '../../../../tests/ThreadsTableTestHelper.js';
import UsersTableTestHelper from '../../../../tests/UsersTableTestHelper.js';
import pool from '../../database/postgres/pool.js';
import LikeRepositoryPostgres from '../LikeRepositoryPostgres.js';

describe('LikeRepositoryPostgres', () => {
  beforeAll(async () => {
    await UsersTableTestHelper.addUser({ id: 'user-123', username: 'dicoding' });
    await UsersTableTestHelper.addUser({ id: 'user-456', username: 'johndoe' });
    await ThreadsTableTestHelper.addThread({ id: 'thread-123', owner: 'user-123' });
    await CommentsTableTestHelper.addComment({ id: 'comment-123', threadId: 'thread-123', owner: 'user-123' });
    await CommentsTableTestHelper.addComment({ id: 'comment-456', threadId: 'thread-123', owner: 'user-456' });
  });

  afterEach(async () => {
    await CommentLikesTableTestHelper.cleanTable();
  });

  afterAll(async () => {
    await CommentsTableTestHelper.cleanTable();
    await ThreadsTableTestHelper.cleanTable();
    await UsersTableTestHelper.cleanTable();
    await pool.end();
  });

  describe('addLike function', () => {
    it('should persist like to database', async () => {
      const fakeIdGenerator = () => '123';
      const likeRepositoryPostgres = new LikeRepositoryPostgres(pool, fakeIdGenerator);

      await likeRepositoryPostgres.addLike('comment-123', 'user-123');

      const likes = await CommentLikesTableTestHelper.findLikeById('like-123');
      expect(likes).toHaveLength(1);
      expect(likes[0].comment_id).toEqual('comment-123');
      expect(likes[0].owner).toEqual('user-123');
    });
  });

  describe('deleteLike function', () => {
    it('should remove like from database', async () => {
      await CommentLikesTableTestHelper.addLike({ id: 'like-123', commentId: 'comment-123', owner: 'user-123' });
      const likeRepositoryPostgres = new LikeRepositoryPostgres(pool, {});

      await likeRepositoryPostgres.deleteLike('comment-123', 'user-123');

      const likes = await CommentLikesTableTestHelper.findLikeByCommentIdAndOwner('comment-123', 'user-123');
      expect(likes).toHaveLength(0);
    });
  });

  describe('verifyLikeExist function', () => {
    it('should return true when like exist', async () => {
      await CommentLikesTableTestHelper.addLike({ id: 'like-123', commentId: 'comment-123', owner: 'user-123' });
      const likeRepositoryPostgres = new LikeRepositoryPostgres(pool, {});

      const isLiked = await likeRepositoryPostgres.verifyLikeExist('comment-123', 'user-123');

      expect(isLiked).toEqual(true);
    });

    it('should return false when like does not exist', async () => {
      const likeRepositoryPostgres = new LikeRepositoryPostgres(pool, {});

      const isLiked = await likeRepositoryPostgres.verifyLikeExist('comment-123', 'user-123');

      expect(isLiked).toEqual(false);
    });
  });

  describe('getLikeCountsByCommentIds function', () => {
    it('should return empty array when comment ids is empty', async () => {
      const likeRepositoryPostgres = new LikeRepositoryPostgres(pool, {});

      const result = await likeRepositoryPostgres.getLikeCountsByCommentIds([]);

      expect(result).toEqual([]);
    });

    it('should return like counts grouped by comment id correctly', async () => {
      await CommentLikesTableTestHelper.addLike({ id: 'like-123', commentId: 'comment-123', owner: 'user-123' });
      await CommentLikesTableTestHelper.addLike({ id: 'like-456', commentId: 'comment-123', owner: 'user-456' });
      await CommentLikesTableTestHelper.addLike({ id: 'like-789', commentId: 'comment-456', owner: 'user-123' });
      const likeRepositoryPostgres = new LikeRepositoryPostgres(pool, {});

      const result = await likeRepositoryPostgres.getLikeCountsByCommentIds(['comment-123', 'comment-456']);

      expect(result).toHaveLength(2);
      expect(result).toEqual(expect.arrayContaining([
        { comment_id: 'comment-123', like_count: 2 },
        { comment_id: 'comment-456', like_count: 1 },
      ]));
    });

    it('should return empty array when no comment has been liked', async () => {
      const likeRepositoryPostgres = new LikeRepositoryPostgres(pool, {});

      const result = await likeRepositoryPostgres.getLikeCountsByCommentIds(['comment-123']);

      expect(result).toEqual([]);
    });
  });
});
