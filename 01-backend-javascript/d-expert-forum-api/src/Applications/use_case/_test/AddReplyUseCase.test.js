import { vi } from 'vitest';
import NewReply from '../../../Domains/replies/entities/NewReply.js';
import AddedReply from '../../../Domains/replies/entities/AddedReply.js';
import ReplyRepository from '../../../Domains/replies/ReplyRepository.js';
import CommentRepository from '../../../Domains/comments/CommentRepository.js';
import ThreadRepository from '../../../Domains/threads/ThreadRepository.js';
import AddReplyUseCase from '../AddReplyUseCase.js';

describe('AddReplyUseCase', () => {
  it('should orchestrating the add reply action correctly', async () => {
    // Arrange
    const useCasePayload = {
      content: 'sebuah balasan',
      threadId: 'thread-123',
      commentId: 'comment-123',
      owner: 'user-123',
    };

    const mockAddedReply = new AddedReply({
      id: 'reply-123',
      content: 'sebuah balasan',
      owner: 'user-123',
    });

    const mockReplyRepository = new ReplyRepository();
    const mockCommentRepository = new CommentRepository();
    const mockThreadRepository = new ThreadRepository();

    mockThreadRepository.verifyAvailableThread = vi.fn().mockImplementation(() => Promise.resolve());
    mockCommentRepository.verifyAvailableComment = vi.fn().mockImplementation(() => Promise.resolve());
    mockReplyRepository.addReply = vi.fn().mockImplementation(() => Promise.resolve(mockAddedReply));

    const addReplyUseCase = new AddReplyUseCase({
      replyRepository: mockReplyRepository,
      commentRepository: mockCommentRepository,
      threadRepository: mockThreadRepository,
    });

    // Action
    const addedReply = await addReplyUseCase.execute(useCasePayload);

    // Assert
    expect(addedReply).toStrictEqual(new AddedReply({
      id: 'reply-123',
      content: 'sebuah balasan',
      owner: 'user-123',
    }));

    expect(mockThreadRepository.verifyAvailableThread).toBeCalledWith(useCasePayload.threadId);
    expect(mockCommentRepository.verifyAvailableComment).toBeCalledWith(useCasePayload.commentId);
    expect(mockReplyRepository.addReply).toBeCalledWith(new NewReply({
      content: useCasePayload.content,
      commentId: useCasePayload.commentId,
      owner: useCasePayload.owner,
    }));
  });

  it('should throw error when payload not contain thread id', async () => {
    // Arrange
    const mockReplyRepository = new ReplyRepository();
    mockReplyRepository.addReply = vi.fn().mockImplementation(() => Promise.resolve());

    const addReplyUseCase = new AddReplyUseCase({
      replyRepository: mockReplyRepository,
      commentRepository: new CommentRepository(),
      threadRepository: new ThreadRepository(),
    });

    // Action & Assert
    await expect(addReplyUseCase.execute({
      content: 'sebuah balasan', commentId: 'comment-123', owner: 'user-123',
    })).rejects.toThrow('ADD_REPLY_USE_CASE.NOT_CONTAIN_NEEDED_PROPERTY');
    expect(mockReplyRepository.addReply).not.toBeCalled();
  });

  it('should throw error when payload not contain needed property', async () => {
    // Arrange
    const mockReplyRepository = new ReplyRepository();
    mockReplyRepository.addReply = vi.fn().mockImplementation(() => Promise.resolve());

    const addReplyUseCase = new AddReplyUseCase({
      replyRepository: mockReplyRepository,
      commentRepository: new CommentRepository(),
      threadRepository: new ThreadRepository(),
    });

    // Action & Assert
    await expect(addReplyUseCase.execute({
      threadId: 'thread-123', commentId: 'comment-123', owner: 'user-123',
    })).rejects.toThrow('NEW_REPLY.NOT_CONTAIN_NEEDED_PROPERTY');
    expect(mockReplyRepository.addReply).not.toBeCalled();
  });
});
