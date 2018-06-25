(ns district-registry.shared.smart-contracts) 

(def smart-contracts 
{:district-factory
 {:name "DistrictFactory",
  :address "0x291bf85c0ae03cc86210fd390a704448f5a0c1af"},
 :trsb
 {:name "TokenReturningStakeBank",
  :address "0x4fdf1cf012d5370fb0f56c734c456f2d5da1306f"},
 :ds-guard
 {:name "DSGuard",
  :address "0x0b84af1165eae22628e1b6488887d6b05ed9aae8"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0xfe3aa02f5e9cb12f35f11e43926a644afc68edb5"},
 :DNT
 {:name "District0xNetworkToken",
  :address "0x3d44fdc0ca745ce1a6a3d02113b3774736d35c78"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x7f5f16fc5427e715e5d285006ce09f3eed687cc7"},
 :param-change
 {:name "ParamChange",
  :address "0xfed3980aea91a2ab6b17e03dca8e39be4ddf52dd"},
 :district-registry
 {:name "Registry",
  :address "0x8a8a678f53111441ea6e54d68ce952abfad08f9e"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0x277329e69a95d516ba060bbb87aaa6f677c6f1d9"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x19e66d157191d6fe2fb306d3b80b04f8a591ead9"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0xccbe6b9a384d48967a9546c463062d04d2faf698"},
 :district-registry-db
 {:name "EternalDb",
  :address "0xcd7967731d8e99a396d392a226e6c16a456fbf5a"},
 :district-registry-fwd
 {:name "MutableForwarder",
  :address "0xbc4377d4e4c74013bab12fdcd0348b594eca3421"},
 :district
 {:name "District",
  :address "0x0f75c88e635fcbffe85f041e17dfc4419be1ccf9"}})