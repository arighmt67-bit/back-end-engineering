import {
  describe, it, expect, vi,
} from 'vitest';
import LikeUnlikeCommentUseCase from '../LikeUnlikeCommentUseCase.js';
import LikeRepository from '../../../Domains/likes/LikeRepository.js';
import CommentRepository from '../../../Domains/comments/CommentRepository.js';
import ThreadRepository from '../../../Domains/threads/ThreadRepository.js';

describe('LikeUnlikeCommentUseCase', () => {
  it('should throw error if use case payload not contain needed property', async () => {
    const useCase = new LikeUnlikeCommentUseCase({});

    await expect(useCase.execute({})).rejects.toThrow('LIKE_UNLIKE_COMMENT_USE_CASE.NOT_CONTAIN_NEEDED_PROPERTY');
    await expect(useCase.execute({ threadId: 'thread-123' })).rejects.toThrow('LIKE_UNLIKE_COMMENT_USE_CASE.NOT_CONTAIN_NEEDED_PROPERTY');
    await expect(useCase.execute({ threadId: 'thread-123', commentId: 'comment-123' })).rejects.toThrow('LIKE_UNLIKE_COMMENT_USE_CASE.NOT_CONTAIN_NEEDED_PROPERTY');
  });

  it('should throw error if payload not meet data type specification', async () => {
    const useCase = new LikeUnlikeCommentUseCase({});

    await expect(useCase.execute({ threadId: 123, commentId: 'comment-123', owner: 'user-123' }))
      .rejects.toThrow('LIKE_UNLIKE_COMMENT_USE_CASE.NOT_MEET_DATA_TYPE_SPECIFICATION');
    await expect(useCase.execute({ threadId: 'thread-123', commentId: {}, owner: 'user-123' }))
      .rejects.toThrow('LIKE_UNLIKE_COMMENT_USE_CASE.NOT_MEET_DATA_TYPE_SPECIFICATION');
    await expect(useCase.execute({ threadId: 'thread-123', commentId: 'comment-123', owner: [] }))
      .rejects.toThrow('LIKE_UNLIKE_COMMENT_USE_CASE.NOT_MEET_DATA_TYPE_SPECIFICATION');
  });

  it('should orchestrating the like action correctly when comment is not liked yet', async () => {
    const useCasePayload = {
      threadId: 'thread-123',
      commentId: 'comment-123',
      owner: 'user-123',
    };

    const mockLikeRepository = new LikeRepository();
    const mockCommentRepository = new CommentRepository();
    const mockThreadRepository = new ThreadRepository();

    mockThreadRepository.verifyAvailableThread = vi.fn(() => Promise.resolve());
    mockCommentRepository.verifyAvailableComment = vi.fn(() => Promise.resolve());
    mockLikeRepository.verifyLikeExist = vi.fn(() => Promise.resolve(false));
    mockLikeRepository.addLike = vi.fn(() => Promise.resolve());
    mockLikeRepository.deleteLike = vi.fn(() => Promise.resolve());

    const useCase = new LikeUnlikeCommentUseCase({
      likeRepository: mockLikeRepository,
      commentRepository: mockCommentRepository,
      threadRepository: mockThreadRepository,
    });

    await useCase.execute(useCasePayload);

    expect(mockThreadRepository.verifyAvailableThread).toHaveBeenCalledWith('thread-123');
    expect(mockCommentRepository.verifyAvailableComment).toHaveBeenCalledWith('comment-123');
    expect(mockLikeRepository.verifyLikeExist).toHaveBeenCalledWith('comment-123', 'user-123');
    expect(mockLikeRepository.addLike).toHaveBeenCalledWith('comment-123', 'user-123');
    expect(mockLikeRepository.deleteLike).not.toHaveBeenCalled();
  });

  it('should orchestrating the unlike action correctly when comment is already liked', async () => {
    const useCasePayload = {
      threadId: 'thread-123',
      commentId: 'comment-123',
      owner: 'user-123',
    };

    const mockLikeRepository = new LikeRepository();
    const mockCommentRepository = new CommentRepository();
    const mockThreadRepository = new ThreadRepository();

    mockThreadRepository.verifyAvailableThread = vi.fn(() => Promise.resolve());
    mockCommentRepository.verifyAvailableComment = vi.fn(() => Promise.resolve());
    mockLikeRepository.verifyLikeExist = vi.fn(() => Promise.resolve(true));
    mockLikeRepository.addLike = vi.fn(() => Promise.resolve());
    mockLikeRepository.deleteLike = vi.fn(() => Promise.resolve());

    const useCase = new LikeUnlikeCommentUseCase({
      likeRepository: mockLikeRepository,
      commentRepository: mockCommentRepository,
      threadRepository: mockThreadRepository,
    });

    await useCase.execute(useCasePayload);

    expect(mockLikeRepository.verifyLikeExist).toHaveBeenCalledWith('comment-123', 'user-123');
    expect(mockLikeRepository.deleteLike).toHaveBeenCalledWith('comment-123', 'user-123');
    expect(mockLikeRepository.addLike).not.toHaveBeenCalled();
  });
});
