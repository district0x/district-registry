(ns district-registry.shared.smart-contracts) 

(def smart-contracts 
{:district-factory
 {:name "DistrictFactory",
  :address "0x57e958d059d7b4ae5221ac4ddcedc2a1f0786f33"},
 :trsb
 {:name "TokenReturningStakeBank",
  :address "0x4fdf1cf012d5370fb0f56c734c456f2d5da1306f"},
 :ds-guard
 {:name "DSGuard",
  :address "0x1e1a252290e2a4caf1f3c0573e331617cb05e12b"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0x44e1f1a1630c99d913565c3bb415aeaef3aad662"},
 :DNT
 {:name "District0xNetworkToken",
  :address "0xeec69e88da3e9ec8b7055bf2ae406353c6a43097"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0xf0257c6e896cbf40ccb9fc2f1d6b4a4967ca3af5"},
 :param-change
 {:name "ParamChange",
  :address "0x7115a0a0972cb1cd974ef643e2b491c67411a4aa"},
 :district-registry
 {:name "Registry",
  :address "0x54cc23c6ff8c353367013c3a4c93072fc8604970"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0x2d4e6e117b6b50492f8094e47de947151e5374be"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0xbe4f486f44b5c1c06ec56ddec9c1e9d658e2787f"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0xbc3c0ebd1d7606a6784eff06b1653cd20a6aa845"},
 :district-registry-db
 {:name "EternalDb",
  :address "0x6efd8dd91b244b64c6352a8055ca149077f4eaa7"},
 :district-registry-fwd
 {:name "MutableForwarder",
  :address "0xf26713c03fe8da271696081c9fa5cbf76812c15e"},
 :district
 {:name "District",
  :address "0xab69e3ac6e68b1a2ffcef68424168eefaf8a63de"}})