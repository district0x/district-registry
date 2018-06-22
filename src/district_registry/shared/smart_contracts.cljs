(ns district-registry.shared.smart-contracts) 

(def smart-contracts 
{:district-config
 {:name "DistrictConfig",
  :address "0xc860ede0ff8a93bc23060f7cfbaf27211decaccb"},
 :ds-guard
 {:name "DSGuard",
  :address "0x95ec19064ff2683c93cf98c5b8dcf944df12477d"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0x51bfc3eb9b8f202320576d6e4f9a2ba077130da5"},
 :DNT
 {:name "District0xNetworkToken",
  :address "0xf2940febc33ecd02cdb1b42f1f4d62a410bacca8"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x21d6b50b6d4f37333a57a3f0f8f8540e6847b0af"},
 :district-registry-db
 {:name "EternalDb",
  :address "0x0490c31ddc3348b1d219e8b1f5157736814ad1e7"},
 :param-change
 {:name "ParamChange",
  :address "0x88cc661820b15ec7e68051e8d69d7be5a01fc234"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0x277329e69a95d516ba060bbb87aaa6f677c6f1d9"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x7f02580c1dcdd09f4ac3f358b4dbd14ddb00efb2"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0xc61966c01ebdeb8274c7f0473a9ea4ba1b534b51"},
 :district-factory
 {:name "DistrictFactory",
  :address "0xdc23be415353a93e3ebc5ba85214b8d045429037"},
 :district-token
 {:name "DistrictToken",
  :address "0xb1804ee895c6c99a9783f059dba5479896c996a8"},
 :district-registry
 {:name "Registry",
  :address "0xa6e58c41a0555e4c0fa1d6599ec9ff5bfda9f413"},
 :district
 {:name "District", :address "0xf8b7956d08b5bd188ba8603a086a7caf724592d1"},
 :district-registry-fwd
 {:name "MutableForwarder",
  :address "0x43a51b5dac414b6f7d5e2b6bbcf6cadc1a7b2931"}})