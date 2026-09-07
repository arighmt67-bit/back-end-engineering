const EventEmitter = require('events');

const BasicMath = {
  add: (a, b) => console.log(a + b),
  subtract: (a, b) => console.log(a - b),
};

const eventEmitter = new EventEmitter();

eventEmitter.on('calculate', BasicMath.add);
eventEmitter.once('calculate', BasicMath.subtract);

eventEmitter.emit('calculate', 2, 3);
eventEmitter.emit('calculate', 5, 6);
