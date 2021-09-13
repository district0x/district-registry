(ns district-registry.shared.smart-contracts-dev)

(def smart-contracts
  {:district-factory
   {:name "DistrictFactory",
    :address "0x0000000000000000000000000000000000000000"},
   :kit-district
   {:name "KitDistrict",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/evm-script-registry-factory
   {:name "EVMScriptRegistryFactory",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/repo
   {:name "Repo",
    :address "0x0000000000000000000000000000000000000000"},
   :ds-guard
   {:name "DSGuard",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/finance
   {:name "Finance",
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
   :aragon/apm-registry
   {:name "APMRegistry",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/dao-factory
   {:name "DAOFactory",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/apm-registry-factory
   {:name "APMRegistryFactory",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/kernel
   {:name "Kernel",
    :address "0x0000000000000000000000000000000000000000"},
   :district-registry
   {:name "Registry",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/vault
   {:name "Vault",
    :address "0x0000000000000000000000000000000000000000"},
   :minime-token-factory
   {:name "MiniMeTokenFactory",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/acl
   {:name "ACL",
    :address "0x0000000000000000000000000000000000000000"},
   :stake-bank
   {:name "StakeBank",
    :address "0x0000000000000000000000000000000000000000"},
   :param-change-factory
   {:name "ParamChangeFactory",
    :address "0x0000000000000000000000000000000000000000"},
   :district-challenge
   {:name "DistrictChallenge",
    :address "0x0000000000000000000000000000000000000000"},
   :param-change-registry-fwd
   {:name "MutableForwarder",
    :address "0x0000000000000000000000000000000000000000",
    :forwards-to :param-change-registry},
   :district-registry-db
   {:name "EternalDb",
    :address "0x0000000000000000000000000000000000000000"},
   :power
   {:name "Power",
    :address "0x0000000000000000000000000000000000000000"},
   :challenge
   {:name "Challenge",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/voting
   {:name "Voting",
    :address "0x0000000000000000000000000000000000000000"},
   :ENS
   {:name "ENS",
    :address "0x0000000000000000000000000000000000000000"},
   :district-registry-fwd
   {:name "MutableForwarder",
    :address "0x0000000000000000000000000000000000000000",
    :forwards-to :district-registry},
   :public-resolver
   {:name "PublicResolver",
    :address "0x0000000000000000000000000000000000000000"},
   :district0x-emails
   {:name "District0xEmails",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/ens-subdomain-registrar
   {:name "ENSSubdomainRegistrar",
    :address "0x0000000000000000000000000000000000000000"},
   :district
   {:name "District",
    :address "0x0000000000000000000000000000000000000000"},
   :aragon/fifs-resolving-registrar
   {:name "FIFSResolvingRegistrar",
    :address "0x0000000000000000000000000000000000000000"}
   :district-registry-legacy
   {:name "RegistryLegacy"
    :address "0x0000000000000000000000000000000000000000"}})
