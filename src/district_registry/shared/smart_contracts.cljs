(ns district-registry.shared.smart-contracts) 

(def smart-contracts 
{:district-factory
 {:name "DistrictFactory",
  :address "0x440e314a32946bb024c9a725b84641e38418fdd7"},
 :trsb
 {:name "TokenReturningStakeBank",
  :address "0x4fdf1cf012d5370fb0f56c734c456f2d5da1306f"},
 :ds-guard
 {:name "DSGuard",
  :address "0x635ade8242973ecd094431e17e24f68de6841d5e"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0x3b5c7d8cf771e86b7ea62600826adb0b96bc3122"},
 :DNT
 {:name "District0xNetworkToken",
  :address "0xd56778470e3d6d30cf507a0d0debdc3533c8d79c"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x3e711f1ca76e2f99642f15ef808016eddb7c370b"},
 :param-change
 {:name "ParamChange",
  :address "0x37697d74813442409b3d2397d8675fd962f7a66e"},
 :district-registry
 {:name "Registry",
  :address "0xb11ba15b42e49ae990c63681fd1f515e0e65be06"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0xc20fbdad204832157685b4e5958f27db2a72fd76"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x6c3671bab767cad516a5de192b9efb7b2095c416"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0xe12ad1bd9c20148892c5cbfaa9ab7b702a614c26"},
 :district-registry-db
 {:name "EternalDb",
  :address "0xed6bc3878c4d13e02bbd518da433c09be8d19169"},
 :district-registry-fwd
 {:name "MutableForwarder",
  :address "0x239b6ad73f39b7eafc6dfa2768ed9b7cda6aa90a"},
 :district
 {:name "District",
  :address "0x7ae7841f0e78d7e6e16c0f0d97476cc950185dc5"}})