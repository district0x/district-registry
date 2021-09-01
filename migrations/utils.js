const fs = require("fs");
const path = require('path');
const edn = require ("jsedn");
const {contracts_build_directory} = require("../truffle.js");


let last = (array) => {
  return array[array.length - 1];
};


let copy = (srcName, dstName, contractsBuildDirectory) => {

  let buildPath = contractsBuildDirectory;

  const srcPath = buildPath + srcName + ".json";
  const dstPath = buildPath + dstName + ".json";

  const data = require(srcPath);
  data.contractName = dstName;

  fs.writeFileSync(dstPath, JSON.stringify(data, null, 2), {flag: "w"});
};


let linkBytecode = (contract, placeholder, replacement) => {
  placeholder = placeholder.replace("0x", "");
  replacement = replacement.replace("0x", "");
  let bytecode = contract.bytecode.split(placeholder).join(replacement);
  contract.bytecode = bytecode;
};


let copyContract = (contractName, contractCopyName) => {
  console.log("Creating Copy of " + contractName + " for deployment...");
  const copyName = contractCopyName || contractName + "_copy";
  console.log("- Contract Name: " + copyName);
  copy(contractName, copyName, contracts_build_directory);
  return copyName;
}


let smartContractsTemplate = (map, env) => {
  return `(ns district-registry.shared.smart-contracts-${env})

(def smart-contracts
  ${map})
`;
};


let encodeContractEDN = (contractInstance, contractName, contractKey, opts) => {
  const cljContractName = ":" + contractKey;
  const contract_address = contractInstance.address.toLowerCase();
  opts = opts || {};

  let entry_value = [
    edn.kw(":name"), contractName,
    edn.kw(":address"), contract_address,
  ];

  // assign a forwards-to optional
  if (opts.forwardsTo !== undefined) {
    entry_value = entry_value.concat([
      edn.kw(":forwards-to"), edn.kw(":" + opts.forwardsTo),
    ]);
  }

  return [
    edn.kw(cljContractName),
    new edn.Map(entry_value),
  ];
};


let readSmartContractsFile = (smartContractsPath) => {
  var content = fs.readFileSync(smartContractsPath, "utf8");

  content = content.replace(/\(ns.*\)/gm, "");
  content = content.replace(/\(def smart-contracts/gm, "");
  content = content.replace(/\)$/gm, "");

  return edn.parse(content);
};


let encodeSmartContracts = (smartContracts) => {
  if (Array.isArray(smartContracts)) {
    smartContracts = new edn.Map(smartContracts);
  }
  var contracts = edn.encode(smartContracts);
  console.log(contracts);
  return contracts;
};


let writeSmartContracts = (smartContractsPath, smartContracts, env) => {
  console.log("Writing to smart contract file: " + smartContractsPath);
  fs.writeFileSync(smartContractsPath, smartContractsTemplate(encodeSmartContracts(smartContracts), env));
};


let getSmartContractAddress = (smartContracts, contractKey) => {
  try {
    return edn.atPath(smartContracts, contractKey + " :address");
  } catch (e) {
    return null;
  }
};

let setSmartContractAddress = (smartContracts, contractKey, newAddress) => {
  var contract = edn.atPath(smartContracts, contractKey);
  contract = contract.set(edn.kw(":address"), newAddress);
  return smartContracts.set(edn.kw(contractKey), contract);
};

kitDistrictAppToNum = {"voting": 0, "vault": 1, "finance": 2};

kitDistrictAppsToNum = (apps) => {
  return apps.map((app) => kitDistrictAppToNum[app]);
}

class Status {
  constructor(id) {
    this.id = id;
    this.currentStep = 0;
    this.lastStep = -1;
    this.values = {};
    this._loadStatus();
  }

  async step(fn) {
    if (this.lastStep < this.currentStep) {
      console.log("Executing step: " + this.currentStep);
      let values = await fn(this);
      Object.assign(this.values, values);
      this.lastStep++;
      this._saveStatus();
    } else {
      console.log("Skipping previously executed step: " + this.currentStep);
    }
    this.currentStep++;
  }

  getValue(key) {
    return this.values[key];
  }

  _filename() {
    return path.resolve(__dirname, this.id + '_status.json');
  }

  _loadStatus() {
    if (fs.existsSync(this._filename())) {
      console.log("Previous execution detected. Loading status to resume")
      try {
        let data = fs.readFileSync(this._filename());
        let st = JSON.parse(data.toString());
        this.lastStep = st['lastStep'];
        this.values = st['values'];
      } catch (err) {
        console.warn("Failed to load status");
      }
    }
  }

  _saveStatus() {
    let data = JSON.stringify({'lastStep': this.lastStep, 'values': this.values});
    try {
      fs.writeFileSync(this._filename(), data)
    } catch (err) {
      console.warn("Cannot save state", err);
    }
  }

  clean() {
    try {
      fs.unlinkSync(this._filename());
    } catch (err) {
      console.warn("Failed to clean status", err);
    }
  }
}

module.exports = {
  last: last,
  copy: copy,
  linkBytecode: linkBytecode,
  copyContract: copyContract,
  smartContractsTemplate: smartContractsTemplate,
  encodeContractEDN: encodeContractEDN,
  readSmartContractsFile: readSmartContractsFile,
  encodeSmartContracts: encodeSmartContracts,
  writeSmartContracts: writeSmartContracts,
  getSmartContractAddress: getSmartContractAddress,
  setSmartContractAddress: setSmartContractAddress,
  kitDistrictAppsToNum: kitDistrictAppsToNum,
  Status: Status
};
