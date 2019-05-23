(ns district-registry.shared.smart-contracts) 

(def smart-contracts 
{:district-factory
 {:name "DistrictFactory",
  :address "0xa94b7f0465e98609391c623d0560c5720a3f2d33"},
 :ds-guard
 {:name "DSGuard",
  :address "0xe78a0f7e598cc8b0bb87894b0f60dd2a88d6a8ab"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0xe982e462b094850f12af94d21d470e21be9d0e9c"},
 :DNT
 {:name "District0xNetworkToken",
  :address "0x254dffcd3277c0b1660f6d42efbb754edababc2b"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0xd833215cbcc3f914bd1c9ece3ee7bf8b14f841bb"},
 :param-change
 {:name "ParamChange",
  :address "0xdb56f2e9369e0d7bd191099125a3f6c370f8ed15"},
 :district-registry
 {:name "Registry",
  :address "0x9561c133dd8580860b6b7e504bc5aa500f0f06a7"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0xcfeb869f69431e42cdb54a4f4f105c19c080a601"},
 :power-factory
 {:name "PowerFactory",
  :address "0xa57b8a5584442b467b4689f1144d269d096a3daf"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x6ed79aa1c71fd7bdbc515efda3bd4e26394435cc"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0x0290fb167208af455bb137780163b7b7a9a10c16"},
 :district-registry-db
 {:name "EternalDb",
  :address "0xc89ce4735882c9f0f0fe26686c53074e09b0d550"},
 :challenge-factory
 {:name "ChallengeFactory",
  :address "0x630589690929e9cdefdef0734717a9ef3ec7fcfe"},
 :district-registry-fwd
 {:name "MutableForwarder",
  :address "0x59d3631c86bbe35ef041872d502f218a39fba150"},
 :stake-bank-factory
 {:name "StakeBankFactory",
  :address "0x26b4afb60d6c903165150c6f0aa14f8016be4aec"},
 :district
 {:name "District",
  :address "0x0e696947a06550def604e82c26fd9e493e576337"}})