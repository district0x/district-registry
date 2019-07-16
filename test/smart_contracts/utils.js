var Promise = require('bluebird');

const increaseTime = Promise.promisify(function(delay, done) {
	web3.currentProvider.send({
		jsonrpc: "2.0",
		"method": "evm_increaseTime",
		params: [delay]}, done)
	}
);

async function expectFailure(call) {
	let tx;
	try {
		tx = await call;
	} catch (error) {
		// Assert ganache revert exception
		assert.equal(
			error.message,
			'Returned error: VM Exception while processing transaction: .'
		);
	}
	if(tx !== undefined) {
		assert.equal(parseInt(tx.receipt.status), 0);
	}
}

module.exports = {
  increaseTime: increaseTime,
  expectFailure: expectFailure
}