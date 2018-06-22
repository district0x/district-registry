(ns district-registry.shared.graphql-schema)

(def graphql-schema "
  scalar Date
  scalar Keyword

  type Query {
    district(regEntry_address: ID!): District
    searchDistricts(statuses: [RegEntryStatus],
                orderBy: DistrictsOrderBy,
                orderDir: OrderDir,
                owner: String,
                creator: String,
                curator: String,
                first: Int,
                after: String
    ): DistrictList

    paramChange(regEntry_address: ID!): ParamChange
    searchParamChanges(
      first: Int,
      after: String
    ): ParamChangeList

    param(db: String!, key: String!): Parameter
    params(db: String!, keys: [String!]): [Parameter]
  }

  enum OrderDir {
    asc
    desc
  }

  enum DistrictsOrderBy {
    districts_orderBy_revealPeriodEnd
    districts_orderBy_commitPeriodEnd
    districts_orderBy_challengePeriodEnd
    districts_orderBy_totalTradeVolume
    districts_orderBy_createdOn
    districts_orderBy_number
    districts_orderBy_totalMinted
  }

  enum RegEntryStatus {
    regEntry_status_challengePeriod
    regEntry_status_commitPeriod
    regEntry_status_revealPeriod
    regEntry_status_blacklisted
    regEntry_status_whitelisted
  }

  interface RegEntry {
    regEntry_address: ID
    regEntry_version: Int
    regEntry_status: RegEntryStatus
    regEntry_creator: String
    regEntry_deposit: Int
    regEntry_createdOn: Date
    regEntry_challengePeriodEnd: Date
    challenge_challenger: String
    challenge_createdOn: Date
    challenge_comment: String
    challenge_votingToken: String
    challenge_rewardPool: Int
    challenge_commitPeriodEnd: Date
    challenge_revealPeriodEnd: Date
    challenge_votesFor: Int
    challenge_votesAgainst: Int
    challenge_votesTotal: Int
    challenge_claimedRewardOn: Date
    challenge_vote(vote_voter: ID!): Vote
  }

  enum VoteOption {
    voteOption_noVote
    voteOption_voteFor
    voteOption_voteAgainst
  }

  type Vote {
    vote_secretHash: String
    vote_option: VoteOption
    vote_amount: Float
    vote_revealedOn: Date
    vote_claimedRewardOn: Date
    vote_reward: Int
  }

  type District implements RegEntry {
    regEntry_address: ID
    regEntry_version: Int
    regEntry_status: RegEntryStatus
    regEntry_creator: String
    regEntry_deposit: Int
    regEntry_createdOn: Date
    regEntry_challengePeriodEnd: Date
    challenge_challenger: String
    challenge_createdOn: Date
    challenge_comment: String
    challenge_votingToken: String
    challenge_rewardPool: Int
    challenge_commitPeriodEnd: Date
    challenge_revealPeriodEnd: Date
    challenge_votesFor: Int
    challenge_votesAgainst: Int
    challenge_votesTotal: Int
    challenge_claimedRewardOn: Date
    challenge_vote(vote_voter: ID!): Vote

    \"Balance of voting token of a voter. This is client-side only, server doesn't return this\"
    challenge_availableVoteAmount(voter: ID!): Int

    district_title: String
    district_number: Int
    district_metaHash: String
    district_imageHash: String
    district_totalSupply: Int
    district_totalMinted: Int
    district_tokenIdStart: Int

    district_totalTradeVolume: Int
    district_totalTradeVolumeRank: Int
  }

  type DistrictList {
    items: [District]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  type ParamChange implements RegEntry {
    regEntry_address: ID
    regEntry_version: Int
    regEntry_status: RegEntryStatus
    regEntry_creator: String
    regEntry_deposit: Int
    regEntry_createdOn: Date
    regEntry_challengePeriodEnd: Date
    challenge_challenger: String
    challenge_createdOn: Date
    challenge_comment: String
    challenge_votingToken: String
    challenge_rewardPool: Int
    challenge_commitPeriodEnd: Date
    challenge_revealPeriodEnd: Date
    challenge_votesFor: Int
    challenge_votesAgainst: Int
    challenge_votesTotal: Int
    challenge_claimedRewardOn: Date
    challenge_vote(vote_voter: ID!): Vote

    \"Balance of voting token of a voter. This is client-side only, server doesn't return this\"
    challenge_availableVoteAmount(voter: ID!): Int

    paramChange_db: String
    paramChange_key: String
    paramChange_value: Int
    paramChange_originalValue: Int
    paramChange_appliedOn: Date
  }

  type ParamChangeList {
    items: [ParamChange]
    totalCount: Int
    endCursor: ID
    hasNextPage: Boolean
  }

  type Parameter {
    param_db: ID
    param_key: ID
    param_value: Float
  }

")
