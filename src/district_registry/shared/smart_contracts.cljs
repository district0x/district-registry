(ns district-registry.shared.smart-contracts) 

(def smart-contracts 
{:district-factory
 {:name "DistrictFactory",
  :address "0x66fa20c4f7a63388f60aad4fdfcffd57cc4f480b"},
 :ds-guard
 {:name "DSGuard",
  :address "0x6bb25ab71f3c92e3f8c64e089f93633725c47655"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0x1b05fd39887e51539e03fc8d1a7c59e9c6b10aec"},
 :DNT
 {:name "District0xNetworkToken",
  :address "0xcef0818f50c30f99063f8928a53e52788d06e764"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x0bffb1ca8365ab797bcc84f5d37f570558ee91f6"},
 :param-change
 {:name "ParamChange",
  :address "0x7b6ed9a1bb8e3377b1f96a3bdef2324e1ea00670"},
 :district-registry
 {:name "Registry",
  :address "0x486c6c8d7dca52127525ec837684ba6d34a95950"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0x04e65bbe9585459d52628040926a19c2bebce8fb"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x2c0325b1654738c42a5048efe0de3244a1384001"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0x9105994844d34d85ff53821e0d12264b2628269b"},
 :district-registry-db
 {:name "EternalDb",
  :address "0xc79ffd1fa4bfcc1dd7683aa3d727b187563c3910"},
 :district-registry-fwd
 {:name "MutableForwarder",
  :address "0x6c0f889157084539c0d676ce80fb50d707a70f1d"},
 :district
 {:name "District",
  :address "0x291fa937564488ee70a5e968aa2f8cd04e3947a9"}})