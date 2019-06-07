(ns district-registry.shared.smart-contracts-prod)

(def smart-contracts 
{:district-factory
 {:name "DistrictFactory",
  :address "0x0000000000000000000000000000000000000000"},
 :ds-guard
 {:name "DSGuard",
  :address "0x0000000000000000000000000000000000000000"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0x0000000000000000000000000000000000000000"},
 :DNT
 {:name "District0xNetworkToken",
  :address "0x0000000000000000000000000000000000000000"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x0000000000000000000000000000000000000000"},
 :param-change
 {:name "ParamChange",
  :address "0x0000000000000000000000000000000000000000"},
 :district-registry
 {:name "Registry",
  :address "0x0000000000000000000000000000000000000000"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0x0000000000000000000000000000000000000000"},
 :power-factory
 {:name "PowerFactory",
  :address "0x0000000000000000000000000000000000000000"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x0000000000000000000000000000000000000000"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0x0000000000000000000000000000000000000000"
  :forwards-to :param-change-registry},
 :district-registry-db
 {:name "EternalDb",
  :address "0x0000000000000000000000000000000000000000"},
 :challenge-factory
 {:name "ChallengeFactory",
  :address "0x0000000000000000000000000000000000000000"},
 :district-registry-fwd
 {:name "MutableForwarder",
  :address "0x0000000000000000000000000000000000000000"
  :forwards-to :district-registry},
 :stake-bank-factory
 {:name "StakeBankFactory",
  :address "0x0000000000000000000000000000000000000000"},
 :district
 {:name "District",
  :address "0x0000000000000000000000000000000000000000"}})