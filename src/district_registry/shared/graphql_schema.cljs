(ns district-registry.shared.graphql-schema)

(def graphql-schema "
  scalar Date
  scalar Keyword

  type Query {
    meme(regEntry_address: ID!): Meme
    searchMemes(statuses: [RegEntryStatus],
                orderBy: MemesOrderBy,
                orderDir: OrderDir,
                owner: String,
                creator: String,
                curator: String,
                first: Int,
                after: String
    ): MemeList

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

  enum MemesOrderBy {
    memes_orderBy_revealPeriodEnd
    memes_orderBy_commitPeriodEnd
    memes_orderBy_challengePeriodEnd
    memes_orderBy_totalTradeVolume
    memes_orderBy_createdOn
    memes_orderBy_number
    memes_orderBy_totalMinted
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

  type Meme implements RegEntry {
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

    meme_title: String
    meme_number: Int
    meme_metaHash: String
    meme_imageHash: String
    meme_totalSupply: Int
    meme_totalMinted: Int
    meme_tokenIdStart: Int

    meme_totalTradeVolume: Int
    meme_totalTradeVolumeRank: Int
  }

  type MemeList {
    items: [Meme]
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
