import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useCounterStore } from '../counter';

describe('Counter Store', () => {
  beforeEach(() => {
    // Create a fresh Pinia instance before each test
    setActivePinia(createPinia());
  });

  it('initialises count at 0', () => {
    const counter = useCounterStore();
    expect(counter.count).toBe(0);
  });

  it('increments count by 1', () => {
    const counter = useCounterStore();
    counter.increment();
    expect(counter.count).toBe(1);
  });

  it('decrements count by 1', () => {
    const counter = useCounterStore();
    counter.increment();
    counter.decrement();
    expect(counter.count).toBe(0);
  });

  it('computes doubleCount correctly', () => {
    const counter = useCounterStore();
    counter.increment();
    counter.increment();
    expect(counter.doubleCount).toBe(4);
  });

  it('resets count to 0', () => {
    const counter = useCounterStore();
    counter.increment();
    counter.increment();
    counter.reset();
    expect(counter.count).toBe(0);
  });
});
