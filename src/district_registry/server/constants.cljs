(ns district-registry.server.constants)


(def web3-events
  {:param-change-db/eternal-db-event [:param-change-registry-db :EternalDbEvent {} {:from-block 0 :to-block "latest"}]
   :param-change-registry/param-change-constructed-event [:param-change-registry-fwd :ParamChangeConstructedEvent {} {:from-block 0 :to-block "latest"}]
   :param-change-registry/param-change-applied-event [:param-change-registry-fwd :ParamChangeAppliedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry-db/eternal-db-event [:district-registry-db :EternalDbEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/district-constructed-event [:district-registry-fwd :DistrictConstructedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/district-meta-hash-changed-event [:district-registry-fwd :DistrictMetaHashChangedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/challenge-created-event [:district-registry-fwd :ChallengeCreatedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/vote-committed-event [:district-registry-fwd :VoteCommittedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/vote-revealed-event [:district-registry-fwd :VoteRevealedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/votes-reclaimed-event [:district-registry-fwd :VotesReclaimedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/vote-reward-claimed-event [:district-registry-fwd :VoteRewardClaimedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/challenger-reward-claimed-event [:district-registry-fwd :ChallengerRewardClaimedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/creator-reward-claimed-event [:district-registry-fwd :CreatorRewardClaimedEvent {} {:from-block 0 :to-block "latest"}]
   :district-registry/stake-changed-event [:district-registry-fwd :DistrictStakeChangedEvent {} {:from-block 0 :to-block "latest"}]})