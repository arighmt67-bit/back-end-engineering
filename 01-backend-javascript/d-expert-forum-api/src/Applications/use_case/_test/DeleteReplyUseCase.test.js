import { vi } from 'vitest';
import ReplyRepository from '../../../Domains/replies/ReplyRepository.js';
import CommentRepository from '../../../Domains/comments/CommentRepository.js';
import ThreadRepository from '../../../Domains/threads/ThreadRepository.js';
import DeleteReplyUseCase from '../DeleteReplyUseCase.js';

describe('DeleteReplyUseCase', () => {
  it('should orchestrating the delete reply action correctly', async () => {
    // Arrange
    const useCasePayload = {
      threadId: 'thread-123',
      commentId: 'comment-123',
      replyId: 'reply-123',
      owner: 'user-123',
    };

    const mockReplyRepository = new ReplyRepository();
    const mockCommentRepository = new CommentRepository();
    const mockThreadRepository = new ThreadRepository();

    mockThreadRepository.verifyAvailableThread = vi.fn().mockImplementation(() => Promise.resolve());
    mockCommentRepository.verifyAvailableComment = vi.fn().mockImplementation(() => Promise.resolve());
    mockReplyRepository.verifyAvailableReply = vi.fn().mockImplementation(() => Promise.resolve());
    mockReplyRepository.verifyReplyOwner = vi.fn().mockImplementation(() => Promise.resolve());
    mockReplyRepository.deleteReplyById = vi.fn().mockImplementation(() => Promise.resolve());

    const deleteReplyUseCase = new DeleteReplyUseCase({
      replyRepository: mockReplyRepository,
      commentRepository: mockCommentRepository,
      threadRepository: mockThreadRepository,
    });

    // Action
    await deleteReplyUseCase.execute(useCasePayload);

    // Assert
    expect(mockThreadRepository.verifyAvailableThread).toBeCalledWith(useCasePayload.threadId);
    expect(mockCommentRepository.verifyAvailableComment).toBeCalledWith(useCasePayload.commentId);
    expect(mockReplyRepository.verifyAvailableReply).toBeCalledWith(useCasePayload.replyId);
    expect(mockReplyRepository.verifyReplyOwner)
      .toBeCalledWith(useCasePayload.replyId, useCasePayload.owner);
    expect(mockReplyRepository.deleteReplyById).toBeCalledWith(useCasePayload.replyId);
  });

  it('should throw error when payload not contain needed property', async () => {
    // Arrange
    const deleteReplyUseCase = new DeleteReplyUseCase({
      replyRepository: {}, commentRepository: {}, threadRepository: {},
    });

    // Action & Assert
    await expect(deleteReplyUseCase.execute({ threadId: 'thread-123', commentId: 'comment-123' }))
      .rejects.toThrow('DELETE_REPLY_USE_CASE.NOT_CONTAIN_NEEDED_PROPERTY');
  });

  it('should throw error when payload not meet data type specification', async () => {
    // Arrange
    const deleteReplyUseCase = new DeleteReplyUseCase({
      replyRepository: {}, commentRepository: {}, threadRepository: {},
    });

    // Action & Assert
    await expect(deleteReplyUseCase.execute({
      threadId: 123, commentId: true, replyId: {}, owner: [],
    })).rejects.toThrow('DELETE_REPLY_USE_CASE.NOT_MEET_DATA_TYPE_SPECIFICATION');
  });
});
