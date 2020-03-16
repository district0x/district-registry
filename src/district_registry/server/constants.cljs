(ns district-registry.server.constants)

(def web3-events
  {:param-change-db/eternal-db-event [:param-change-registry-db :EternalDbEvent]
   :param-change-registry/param-change-constructed-event [:param-change-registry-fwd :ParamChangeConstructedEvent]
   :param-change-registry/param-change-applied-event [:param-change-registry-fwd :ParamChangeAppliedEvent]
   :district-registry-db/eternal-db-event [:district-registry-db :EternalDbEvent]
   :district-registry/district-constructed-event [:district-registry-fwd :DistrictConstructedEvent]
   :district-registry/district-meta-hash-changed-event [:district-registry-fwd :DistrictMetaHashChangedEvent]
   :district-registry/challenge-created-event [:district-registry-fwd :ChallengeCreatedEvent]
   :district-registry/vote-committed-event [:district-registry-fwd :VoteCommittedEvent]
   :district-registry/vote-revealed-event [:district-registry-fwd :VoteRevealedEvent]
   :district-registry/votes-reclaimed-event [:district-registry-fwd :VotesReclaimedEvent]
   :district-registry/vote-reward-claimed-event [:district-registry-fwd :VoteRewardClaimedEvent]
   :district-registry/challenger-reward-claimed-event [:district-registry-fwd :ChallengerRewardClaimedEvent]
   :district-registry/creator-reward-claimed-event [:district-registry-fwd :CreatorRewardClaimedEvent]
   :district-registry/stake-changed-event [:district-registry-fwd :DistrictStakeChangedEvent]})
