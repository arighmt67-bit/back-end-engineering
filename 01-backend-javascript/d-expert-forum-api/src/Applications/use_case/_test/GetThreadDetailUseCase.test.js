/* eslint-disable camelcase -- properti mengikuti nama kolom pada basis data */
import { vi } from 'vitest';
import ThreadRepository from '../../../Domains/threads/ThreadRepository.js';
import CommentRepository from '../../../Domains/comments/CommentRepository.js';
import ReplyRepository from '../../../Domains/replies/ReplyRepository.js';
import LikeRepository from '../../../Domains/likes/LikeRepository.js';
import GetThreadDetailUseCase from '../GetThreadDetailUseCase.js';

describe('GetThreadDetailUseCase', () => {
  it('should orchestrating the get thread detail action correctly', async () => {
    // Arrange
    const useCasePayload = { threadId: 'thread-123' };

    /** data mentah dari repository (nilai netral, bukan expected value) */
    const retrievedThread = {
      id: 'thread-123',
      title: 'sebuah thread',
      body: 'sebuah body thread',
      date: '2021-08-08T07:19:09.775Z',
      username: 'dicoding',
    };

    const retrievedComments = [
      {
        id: 'comment-123',
        username: 'johndoe',
        date: '2021-08-08T07:22:33.555Z',
        content: 'sebuah comment',
        is_delete: false,
      },
      {
        id: 'comment-456',
        username: 'dicoding',
        date: '2021-08-08T07:26:21.338Z',
        content: 'comment asli sebelum dihapus',
        is_delete: true,
      },
    ];

    const retrievedReplies = [
      {
        id: 'reply-123',
        content: 'balasan asli sebelum dihapus',
        date: '2021-08-08T07:59:48.766Z',
        comment_id: 'comment-123',
        is_delete: true,
        username: 'johndoe',
      },
      {
        id: 'reply-456',
        content: 'sebuah balasan',
        date: '2021-08-08T08:07:01.522Z',
        comment_id: 'comment-123',
        is_delete: false,
        username: 'dicoding',
      },
    ];

    /**
     * comment-123 memiliki dua suka, sedangkan comment-456 tidak memiliki
     * baris sama sekali pada tabel comment_likes. Kasus kedua memastikan
     * likeCount tetap bernilai 0, bukan undefined.
     */
    const retrievedLikeCounts = [
      { comment_id: 'comment-123', like_count: 2 },
    ];

    const mockThreadRepository = new ThreadRepository();
    const mockCommentRepository = new CommentRepository();
    const mockReplyRepository = new ReplyRepository();
    const mockLikeRepository = new LikeRepository();

    mockThreadRepository.getThreadById = vi.fn().mockImplementation(() => Promise.resolve(retrievedThread));
    mockCommentRepository.getCommentsByThreadId = vi.fn().mockImplementation(() => Promise.resolve(retrievedComments));
    mockReplyRepository.getRepliesByCommentIds = vi.fn().mockImplementation(() => Promise.resolve(retrievedReplies));
    mockLikeRepository.getLikeCountsByCommentIds = vi.fn().mockImplementation(() => Promise.resolve(retrievedLikeCounts));

    const getThreadDetailUseCase = new GetThreadDetailUseCase({
      threadRepository: mockThreadRepository,
      commentRepository: mockCommentRepository,
      replyRepository: mockReplyRepository,
      likeRepository: mockLikeRepository,
    });

    // Action
    const thread = await getThreadDetailUseCase.execute(useCasePayload);

    // Assert
    expect(thread).toStrictEqual({
      id: 'thread-123',
      title: 'sebuah thread',
      body: 'sebuah body thread',
      date: '2021-08-08T07:19:09.775Z',
      username: 'dicoding',
      comments: [
        {
          id: 'comment-123',
          username: 'johndoe',
          date: '2021-08-08T07:22:33.555Z',
          replies: [
            {
              id: 'reply-123',
              content: '**balasan telah dihapus**',
              date: '2021-08-08T07:59:48.766Z',
              username: 'johndoe',
            },
            {
              id: 'reply-456',
              content: 'sebuah balasan',
              date: '2021-08-08T08:07:01.522Z',
              username: 'dicoding',
            },
          ],
          content: 'sebuah comment',
          likeCount: 2,
        },
        {
          id: 'comment-456',
          username: 'dicoding',
          date: '2021-08-08T07:26:21.338Z',
          replies: [],
          content: '**komentar telah dihapus**',
          likeCount: 0,
        },
      ],
    });

    expect(mockThreadRepository.getThreadById).toBeCalledWith(useCasePayload.threadId);
    expect(mockCommentRepository.getCommentsByThreadId).toBeCalledWith(useCasePayload.threadId);
    expect(mockReplyRepository.getRepliesByCommentIds).toBeCalledWith(['comment-123', 'comment-456']);
    expect(mockLikeRepository.getLikeCountsByCommentIds).toBeCalledWith(['comment-123', 'comment-456']);
  });

  it('should throw error when payload not contain thread id', async () => {
    // Arrange
    const getThreadDetailUseCase = new GetThreadDetailUseCase({
      threadRepository: {}, commentRepository: {}, replyRepository: {}, likeRepository: {},
    });

    // Action & Assert
    await expect(getThreadDetailUseCase.execute({}))
      .rejects.toThrow('GET_THREAD_DETAIL_USE_CASE.NOT_CONTAIN_THREAD_ID');
  });

  it('should throw error when thread id not string', async () => {
    // Arrange
    const getThreadDetailUseCase = new GetThreadDetailUseCase({
      threadRepository: {}, commentRepository: {}, replyRepository: {}, likeRepository: {},
    });

    // Action & Assert
    await expect(getThreadDetailUseCase.execute({ threadId: 123 }))
      .rejects.toThrow('GET_THREAD_DETAIL_USE_CASE.PAYLOAD_NOT_MEET_DATA_TYPE_SPECIFICATION');
  });
});
