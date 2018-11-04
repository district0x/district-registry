(ns
 district-registry.shared.graphql-specs
 (:require [bignumber.core] [clojure.spec.alpha]))

(clojure.spec.alpha/def
 :challenge/challenger
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/ID)))

(clojure.spec.alpha/def
 :challenge/challenger'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :challenge/claimed-reward-on
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Date)))

(clojure.spec.alpha/def
 :challenge/claimed-reward-on'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :challenge/commit-period-end
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Date)))

(clojure.spec.alpha/def
 :challenge/commit-period-end'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :challenge/created-on
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Date)))

(clojure.spec.alpha/def
 :challenge/created-on'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :challenge/reveal-period-end
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Date)))

(clojure.spec.alpha/def
 :challenge/reveal-period-end'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :challenge/reward-pool
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :challenge/reward-pool'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :challenge/vote
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Vote)))

(clojure.spec.alpha/def
 :challenge/vote'args
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.Challenge.challenge_vote/voter]
  :req-un
  []))

(clojure.spec.alpha/def
 :challenge/votes-exclude
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :challenge/votes-exclude'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :challenge/votes-include
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :challenge/votes-include'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :challenge/votes-total
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :challenge/votes-total'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/background-image-hash
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :district/background-image-hash'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/balance-of
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :district/balance-of'args
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.District.district_balanceOf/staker]
  :req-un
  []))

(clojure.spec.alpha/def
 :district/description
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :district/description'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/dnt-staked
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :district/dnt-staked'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/dnt-staked-for
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :district/dnt-staked-for'args
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.District.district_dntStakedFor/staker]
  :req-un
  []))

(clojure.spec.alpha/def
 :district/dnt-weight
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Int)))

(clojure.spec.alpha/def
 :district/dnt-weight'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/github-url
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :district/github-url'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/info-hash
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :district/info-hash'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/logo-image-hash
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :district/logo-image-hash'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/name
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :district/name'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/total-supply
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :district/total-supply'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :district/url
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :district/url'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :districts.order-by/commit-period-end
 #{"districts_orderBy_commitPeriodEnd"})

(clojure.spec.alpha/def
 :districts.order-by/created-on
 #{"districts_orderBy_createdOn"})

(clojure.spec.alpha/def
 :districts.order-by/dnt-staked
 #{"districts_orderBy_dntStaked"})

(clojure.spec.alpha/def
 :districts.order-by/reveal-period-end
 #{"districts_orderBy_revealPeriodEnd"})

(clojure.spec.alpha/def
 :districts.order-by/total-supply
 #{"districts_orderBy_totalSupply"})

(clojure.spec.alpha/def
 :gql/BigNumber
 (clojure.spec.alpha/and clojure.core/any? bignumber.core/bignumber?))

(clojure.spec.alpha/def :gql/Boolean clojure.core/boolean?)

(clojure.spec.alpha/def
 :gql/Challenge
 (clojure.spec.alpha/keys
  :opt-un
  [:challenge/challenger
   :challenge/claimed-reward-on
   :challenge/commit-period-end
   :challenge/created-on
   :challenge/reveal-period-end
   :challenge/reward-pool
   :challenge/vote
   :challenge/votes-exclude
   :challenge/votes-include
   :challenge/votes-total
   :gql.Challenge/__typename]))

(clojure.spec.alpha/def
 :gql/Config
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.Config/ipfs :gql.Config/__typename]))

(clojure.spec.alpha/def :gql/Date clojure.core/any?)

(clojure.spec.alpha/def
 :gql/District
 (clojure.spec.alpha/keys
  :opt-un
  [:district/background-image-hash
   :district/balance-of
   :district/description
   :district/dnt-staked
   :district/dnt-staked-for
   :district/dnt-weight
   :district/github-url
   :district/info-hash
   :district/logo-image-hash
   :district/name
   :district/total-supply
   :district/url
   :reg-entry/address
   :reg-entry/challenge-period-end
   :reg-entry/challenges
   :reg-entry/created-on
   :reg-entry/creator
   :reg-entry/deposit
   :reg-entry/status
   :reg-entry/version
   :gql.District/__typename]))

(clojure.spec.alpha/def
 :gql/DistrictList
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.DistrictList/endCursor
   :gql.DistrictList/hasNextPage
   :gql.DistrictList/items
   :gql.DistrictList/totalCount
   :gql.DistrictList/__typename]))

(clojure.spec.alpha/def
 :gql/DistrictsOrderBy
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :districts_orderBy_commitPeriodEnd
   :districts.order-by/commit-period-end
   :districts_orderBy_createdOn
   :districts.order-by/created-on
   :districts_orderBy_dntStaked
   :districts.order-by/dnt-staked
   :districts_orderBy_revealPeriodEnd
   :districts.order-by/reveal-period-end
   :districts_orderBy_totalSupply
   :districts.order-by/total-supply)))

(clojure.spec.alpha/def :gql/Float clojure.core/float?)

(clojure.spec.alpha/def :gql/ID clojure.core/string?)

(clojure.spec.alpha/def :gql/Int clojure.core/integer?)

(clojure.spec.alpha/def
 :gql/Ipfs
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.Ipfs/endpoint
   :gql.Ipfs/gateway
   :gql.Ipfs/host
   :gql.Ipfs/__typename]))

(clojure.spec.alpha/def :gql/Keyword clojure.core/any?)

(clojure.spec.alpha/def
 :gql/OrderDir
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :asc
   :gql.OrderDir/asc
   :desc
   :gql.OrderDir/desc)))

(clojure.spec.alpha/def
 :gql/Param
 (clojure.spec.alpha/keys
  :opt-un
  [:param/db :param/key :param/value :gql.Param/__typename]))

(clojure.spec.alpha/def
 :gql/ParamChange
 (clojure.spec.alpha/keys
  :opt-un
  [:param-change/applied-on
   :param-change/db
   :param-change/key
   :param-change/original-value
   :param-change/value
   :reg-entry/address
   :reg-entry/challenge-period-end
   :reg-entry/challenges
   :reg-entry/created-on
   :reg-entry/creator
   :reg-entry/deposit
   :reg-entry/status
   :reg-entry/version
   :gql.ParamChange/__typename]))

(clojure.spec.alpha/def
 :gql/ParamChangeList
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.ParamChangeList/endCursor
   :gql.ParamChangeList/hasNextPage
   :gql.ParamChangeList/items
   :gql.ParamChangeList/totalCount
   :gql.ParamChangeList/__typename]))

(clojure.spec.alpha/def
 :gql/ParamChangesGroupBy
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :paramChanges_groupBy_key
   :param-changes.group-by/key)))

(clojure.spec.alpha/def
 :gql/ParamChangesOrderBy
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :paramChanges_orderBy_appliedOn
   :param-changes.order-by/applied-on)))

(clojure.spec.alpha/def
 :gql/Query
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.Query/config
   :gql.Query/district
   :gql.Query/param
   :gql.Query/paramChange
   :gql.Query/params
   :gql.Query/searchDistricts
   :gql.Query/searchParamChanges
   :gql.Query/__typename]))

(clojure.spec.alpha/def
 :gql/QueryRoot
 (clojure.spec.alpha/keys :opt-un [:gql.QueryRoot/__typename]))

(clojure.spec.alpha/def
 :gql/RegEntry
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :District
   :gql/District
   :ParamChange
   :gql/ParamChange)))

(clojure.spec.alpha/def
 :gql/RegEntryStatus
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :regEntry_status_blacklisted
   :reg-entry.status/blacklisted
   :regEntry_status_challengePeriod
   :reg-entry.status/challenge-period
   :regEntry_status_commitPeriod
   :reg-entry.status/commit-period
   :regEntry_status_revealPeriod
   :reg-entry.status/reveal-period
   :regEntry_status_whitelisted
   :reg-entry.status/whitelisted)))

(clojure.spec.alpha/def :gql/String clojure.core/string?)

(clojure.spec.alpha/def
 :gql/Vote
 (clojure.spec.alpha/keys
  :opt-un
  [:vote/amount
   :vote/claimed-reward-on
   :vote/option
   :vote/revealed-on
   :vote/reward
   :vote/secret-hash
   :gql.Vote/__typename]))

(clojure.spec.alpha/def
 :gql/VoteOption
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :voteOption_exclude
   :vote-option/exclude
   :voteOption_include
   :vote-option/include
   :voteOption_neither
   :vote-option/neither)))

(clojure.spec.alpha/def :gql.Challenge/__typename #{"Challenge"})

(clojure.spec.alpha/def
 :gql.Challenge/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.Challenge.challenge_vote/voter
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/ID)))

(clojure.spec.alpha/def :gql.Config/__typename #{"Config"})

(clojure.spec.alpha/def
 :gql.Config/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.Config/ipfs
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Ipfs)))

(clojure.spec.alpha/def
 :gql.Config/ipfs'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def :gql.District/__typename #{"District"})

(clojure.spec.alpha/def
 :gql.District/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.District.district_balanceOf/staker
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/ID)))

(clojure.spec.alpha/def
 :gql.District.district_dntStakedFor/staker
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/ID)))

(clojure.spec.alpha/def :gql.DistrictList/__typename #{"DistrictList"})

(clojure.spec.alpha/def
 :gql.DistrictList/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.DistrictList/endCursor
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :gql.DistrictList/endCursor'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.DistrictList/hasNextPage
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/Boolean)))

(clojure.spec.alpha/def
 :gql.DistrictList/hasNextPage'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.DistrictList/items
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   (clojure.spec.alpha/coll-of
    (clojure.spec.alpha/nonconforming
     (clojure.spec.alpha/or
      :null
      clojure.core/nil?
      :non-null
      :gql/District))
    :kind
    clojure.core/sequential?))))

(clojure.spec.alpha/def
 :gql.DistrictList/items'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.DistrictList/totalCount
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Int)))

(clojure.spec.alpha/def
 :gql.DistrictList/totalCount'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def :gql.Ipfs/__typename #{"Ipfs"})

(clojure.spec.alpha/def
 :gql.Ipfs/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.Ipfs/endpoint
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :gql.Ipfs/endpoint'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.Ipfs/gateway
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :gql.Ipfs/gateway'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.Ipfs/host
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :gql.Ipfs/host'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def :gql.OrderDir/asc #{"asc"})

(clojure.spec.alpha/def :gql.OrderDir/desc #{"desc"})

(clojure.spec.alpha/def :gql.Param/__typename #{"Param"})

(clojure.spec.alpha/def
 :gql.Param/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def :gql.ParamChange/__typename #{"ParamChange"})

(clojure.spec.alpha/def
 :gql.ParamChange/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.ParamChangeList/__typename
 #{"ParamChangeList"})

(clojure.spec.alpha/def
 :gql.ParamChangeList/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.ParamChangeList/endCursor
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/ID)))

(clojure.spec.alpha/def
 :gql.ParamChangeList/endCursor'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.ParamChangeList/hasNextPage
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/Boolean)))

(clojure.spec.alpha/def
 :gql.ParamChangeList/hasNextPage'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.ParamChangeList/items
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   (clojure.spec.alpha/coll-of
    (clojure.spec.alpha/nonconforming
     (clojure.spec.alpha/or
      :null
      clojure.core/nil?
      :non-null
      :gql/ParamChange))
    :kind
    clojure.core/sequential?))))

(clojure.spec.alpha/def
 :gql.ParamChangeList/items'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.ParamChangeList/totalCount
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Int)))

(clojure.spec.alpha/def
 :gql.ParamChangeList/totalCount'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def :gql.Query/__typename #{"Query"})

(clojure.spec.alpha/def
 :gql.Query/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.Query/config
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/Config)))

(clojure.spec.alpha/def
 :gql.Query/config'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :gql.Query/district
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/District)))

(clojure.spec.alpha/def
 :gql.Query/district'args
 (clojure.spec.alpha/keys :opt-un [] :req-un [:reg-entry/address]))

(clojure.spec.alpha/def
 :gql.Query/param
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Param)))

(clojure.spec.alpha/def
 :gql.Query/param'args
 (clojure.spec.alpha/keys
  :opt-un
  []
  :req-un
  [:gql.Query.param/db :gql.Query.param/key]))

(clojure.spec.alpha/def
 :gql.Query/paramChange
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/ParamChange)))

(clojure.spec.alpha/def
 :gql.Query/paramChange'args
 (clojure.spec.alpha/keys :opt-un [] :req-un [:reg-entry/address]))

(clojure.spec.alpha/def
 :gql.Query/params
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   (clojure.spec.alpha/coll-of
    (clojure.spec.alpha/nonconforming
     (clojure.spec.alpha/or
      :null
      clojure.core/nil?
      :non-null
      :gql/Param))
    :kind
    clojure.core/sequential?))))

(clojure.spec.alpha/def
 :gql.Query/params'args
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.Query.params/keys]
  :req-un
  [:gql.Query.params/db]))

(clojure.spec.alpha/def
 :gql.Query/searchDistricts
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/DistrictList)))

(clojure.spec.alpha/def
 :gql.Query/searchDistricts'args
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.Query.searchDistricts/after
   :gql.Query.searchDistricts/first
   :gql.Query.searchDistricts/orderBy
   :gql.Query.searchDistricts/orderDir
   :gql.Query.searchDistricts/statuses]
  :req-un
  []))

(clojure.spec.alpha/def
 :gql.Query/searchParamChanges
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/ParamChangeList)))

(clojure.spec.alpha/def
 :gql.Query/searchParamChanges'args
 (clojure.spec.alpha/keys
  :opt-un
  [:gql.Query.searchParamChanges/after
   :gql.Query.searchParamChanges/first
   :gql.Query.searchParamChanges/groupBy
   :gql.Query.searchParamChanges/orderBy
   :gql.Query.searchParamChanges/orderDir]
  :req-un
  [:gql.Query.searchParamChanges/db :gql.Query.searchParamChanges/key]))

(clojure.spec.alpha/def
 :gql.Query.params/keys
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   (clojure.spec.alpha/coll-of
    :gql/String
    :kind
    clojure.core/sequential?))))

(clojure.spec.alpha/def
 :gql.Query.searchDistricts/after
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :gql.Query.searchDistricts/first
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Int)))

(clojure.spec.alpha/def
 :gql.Query.searchDistricts/orderBy
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/DistrictsOrderBy)))

(clojure.spec.alpha/def
 :gql.Query.searchDistricts/orderDir
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/OrderDir)))

(clojure.spec.alpha/def
 :gql.Query.searchDistricts/statuses
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   (clojure.spec.alpha/coll-of
    (clojure.spec.alpha/nonconforming
     (clojure.spec.alpha/or
      :null
      clojure.core/nil?
      :non-null
      :gql/RegEntryStatus))
    :kind
    clojure.core/sequential?))))

(clojure.spec.alpha/def
 :gql.Query.searchParamChanges/after
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :gql.Query.searchParamChanges/first
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Int)))

(clojure.spec.alpha/def
 :gql.Query.searchParamChanges/groupBy
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/ParamChangesGroupBy)))

(clojure.spec.alpha/def
 :gql.Query.searchParamChanges/orderBy
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/ParamChangesOrderBy)))

(clojure.spec.alpha/def
 :gql.Query.searchParamChanges/orderDir
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/OrderDir)))

(clojure.spec.alpha/def :gql.QueryRoot/__typename #{"QueryRoot"})

(clojure.spec.alpha/def
 :gql.QueryRoot/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def :gql.Vote/__typename #{"Vote"})

(clojure.spec.alpha/def
 :gql.Vote/__typename'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :param/db
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/ID)))

(clojure.spec.alpha/def
 :param/db'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :param/key
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/ID)))

(clojure.spec.alpha/def
 :param/key'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :param/value
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Float)))

(clojure.spec.alpha/def
 :param/value'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :param-change/applied-on
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Date)))

(clojure.spec.alpha/def
 :param-change/applied-on'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :param-change/db
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :param-change/db'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :param-change/key
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :param-change/key'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :param-change/original-value
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :param-change/original-value'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :param-change/value
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :param-change/value'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :param-changes.group-by/key
 #{"paramChanges_groupBy_key"})

(clojure.spec.alpha/def
 :param-changes.order-by/applied-on
 #{"paramChanges_orderBy_appliedOn"})

(clojure.spec.alpha/def
 :reg-entry/address
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/ID)))

(clojure.spec.alpha/def
 :reg-entry/address'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :reg-entry/challenge-period-end
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Date)))

(clojure.spec.alpha/def
 :reg-entry/challenge-period-end'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :reg-entry/challenges
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   (clojure.spec.alpha/coll-of
    (clojure.spec.alpha/nonconforming
     (clojure.spec.alpha/or
      :null
      clojure.core/nil?
      :non-null
      :gql/Challenge))
    :kind
    clojure.core/sequential?))))

(clojure.spec.alpha/def
 :reg-entry/challenges'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :reg-entry/created-on
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Date)))

(clojure.spec.alpha/def
 :reg-entry/created-on'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :reg-entry/creator
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/ID)))

(clojure.spec.alpha/def
 :reg-entry/creator'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :reg-entry/deposit
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :reg-entry/deposit'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :reg-entry/status
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/RegEntryStatus)))

(clojure.spec.alpha/def
 :reg-entry/status'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :reg-entry/version
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Int)))

(clojure.spec.alpha/def
 :reg-entry/version'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :reg-entry.status/blacklisted
 #{"regEntry_status_blacklisted"})

(clojure.spec.alpha/def
 :reg-entry.status/challenge-period
 #{"regEntry_status_challengePeriod"})

(clojure.spec.alpha/def
 :reg-entry.status/commit-period
 #{"regEntry_status_commitPeriod"})

(clojure.spec.alpha/def
 :reg-entry.status/reveal-period
 #{"regEntry_status_revealPeriod"})

(clojure.spec.alpha/def
 :reg-entry.status/whitelisted
 #{"regEntry_status_whitelisted"})

(clojure.spec.alpha/def
 :vote/amount
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :vote/amount'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :vote/claimed-reward-on
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Date)))

(clojure.spec.alpha/def
 :vote/claimed-reward-on'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :vote/option
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/VoteOption)))

(clojure.spec.alpha/def
 :vote/option'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :vote/revealed-on
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or :null clojure.core/nil? :non-null :gql/Date)))

(clojure.spec.alpha/def
 :vote/revealed-on'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :vote/reward
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/BigNumber)))

(clojure.spec.alpha/def
 :vote/reward'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def
 :vote/secret-hash
 (clojure.spec.alpha/nonconforming
  (clojure.spec.alpha/or
   :null
   clojure.core/nil?
   :non-null
   :gql/String)))

(clojure.spec.alpha/def
 :vote/secret-hash'args
 (clojure.spec.alpha/keys :opt-un [] :req-un []))

(clojure.spec.alpha/def :vote-option/exclude #{"voteOption_exclude"})

(clojure.spec.alpha/def :vote-option/include #{"voteOption_include"})

(clojure.spec.alpha/def :vote-option/neither #{"voteOption_neither"})

(clojure.spec.alpha/def :gql.Query.param/db :gql/String)

(clojure.spec.alpha/def :gql.Query.param/key :gql/String)

(clojure.spec.alpha/def :gql.Query.params/db :gql/String)

(clojure.spec.alpha/def :gql.Query.searchParamChanges/db :gql/String)

(clojure.spec.alpha/def :gql.Query.searchParamChanges/key :gql/String)
