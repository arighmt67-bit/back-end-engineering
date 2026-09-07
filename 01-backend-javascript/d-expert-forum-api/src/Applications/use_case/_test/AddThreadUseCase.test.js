import { vi } from 'vitest';
import NewThread from '../../../Domains/threads/entities/NewThread.js';
import AddedThread from '../../../Domains/threads/entities/AddedThread.js';
import ThreadRepository from '../../../Domains/threads/ThreadRepository.js';
import AddThreadUseCase from '../AddThreadUseCase.js';

describe('AddThreadUseCase', () => {
  it('should orchestrating the add thread action correctly', async () => {
    // Arrange
    const useCasePayload = {
      title: 'sebuah thread',
      body: 'sebuah body thread',
      owner: 'user-123',
    };

    /** nilai kembalian mock bersifat netral, dibuat terpisah dari expected value */
    const mockAddedThread = new AddedThread({
      id: 'thread-123',
      title: 'sebuah thread',
      owner: 'user-123',
    });

    const mockThreadRepository = new ThreadRepository();
    mockThreadRepository.addThread = vi.fn().mockImplementation(() => Promise.resolve(mockAddedThread));

    const addThreadUseCase = new AddThreadUseCase({ threadRepository: mockThreadRepository });

    // Action
    const addedThread = await addThreadUseCase.execute(useCasePayload);

    // Assert
    expect(addedThread).toStrictEqual(new AddedThread({
      id: 'thread-123',
      title: 'sebuah thread',
      owner: 'user-123',
    }));

    expect(mockThreadRepository.addThread).toBeCalledWith(new NewThread({
      title: useCasePayload.title,
      body: useCasePayload.body,
      owner: useCasePayload.owner,
    }));
  });

  it('should throw error when payload not contain needed property', async () => {
    // Arrange
    const mockThreadRepository = new ThreadRepository();
    mockThreadRepository.addThread = vi.fn().mockImplementation(() => Promise.resolve());
    const addThreadUseCase = new AddThreadUseCase({ threadRepository: mockThreadRepository });

    // Action & Assert
    await expect(addThreadUseCase.execute({ title: 'sebuah thread' }))
      .rejects.toThrow('NEW_THREAD.NOT_CONTAIN_NEEDED_PROPERTY');
    expect(mockThreadRepository.addThread).not.toBeCalled();
  });
});
