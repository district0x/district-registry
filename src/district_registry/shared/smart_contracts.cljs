(ns district-registry.shared.smart-contracts) 

(def smart-contracts 
{:district-factory
 {:name "DistrictFactory",
  :address "0x9e90054f4b6730cffaf1e6f6ea10e1bf9dd26dbb"},
 :ds-guard
 {:name "DSGuard",
  :address "0x4cfb3f70bf6a80397c2e634e5bdd85bc0bb189ee"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0xe97dbd7116d168190f8a6e7beb1092c103c53a12"},
 :DNT
 {:name "District0xNetworkToken",
  :address "0x47a2db5d68751eeadfbc44851e84acdb4f7299cc"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0xeea2fc1d255fd28aa15c6c2324ad40b03267f9c5"},
 :param-change
 {:name "ParamChange",
  :address "0xdfccc9c59c7361307d47c558ffa75840b32dba29"},
 :district-registry
 {:name "Registry",
  :address "0xc34175a79acca40392becd22ff10faebfe780ae7"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0xceefd27e0542afa926b87d23936c79c276a48277"},
 :power-factory
 {:name "PowerFactory",
  :address "0x2c5f3c004878923f55a2a255f89fe29393177509"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0xfe82e8f24a51e670133f4268cdfc164c49fc3b37"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0xd13ebb5c39fb00c06122827e1cbd389930c9e0e3"},
 :district-registry-db
 {:name "EternalDb",
  :address "0x988b6cfbf3332ff98ffbded665b1f53a61f92612"},
 :challenge-factory
 {:name "ChallengeFactory",
  :address "0x966d3e76e7a890a2d7b9ae1e370dc219e920f9d4"},
 :district-registry-fwd
 {:name "MutableForwarder",
  :address "0xf16165f1046f1b3cdb37da25e835b986e696313a"},
 :stake-bank-factory
 {:name "StakeBankFactory",
  :address "0x2f3efa6bbdc5faf4dc1a600765c7b7829e47be10"},
 :district
 {:name "District",
  :address "0x1abe68277ae236083947f2551fee8b885efca8f5"}})