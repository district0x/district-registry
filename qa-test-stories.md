## QA Test Stories
Following user stories should be tested on QA before updates can be pushed into production.

### User Story #1 
- Go to My Account Page Email and submit your email for 2 different addresses
- Go to Submit Page
- Fill out the form with random data and choose Flat curve
- After tx is processed go to home page and verify is the submission is listed
- From home page, stake and unstake into the district random amounts, randomly many times
- Go to detail page and verify is all displayed data match your submitted data
- From detail page, stake and unstake into the district random amounts, randomly many times, from random addresses
- Looking at stake history chart, verify if staking follows chosen curve
- Switch to a different address and challenge the district
- From challenger's address vote Against with random amount
- From creator's address vote For with larger amount than you voted Against
- Verify if you received email notification about revealing your vote
- Reveal votes from both addresses
- Verify if the district is still in registry after reveal period finished
- Verify if the creator's address is egligible challenge as well as vote reward
- Claim the reward from creator's address
- Verify if you received email notification about claimed reward
- Verify that challenger's address is not egligible for any reward
- Stake and unstake into the district random amounts, randomly many times
- Switch to a challenger's address and challenge the district again
- From challenger's address vote Against with larger amount than currently staked balance
- Reveal the vote
- Verify is the district is blacklisted after reveal period finished
- Verify that challenger's address is egligible for challenge as well as vote reward
- Claim the reward from challenger's address
- Verify if you received email notification about claimed reward
- Go to My Account - Activity and verify is all transactions you submitted are listed there

### User Story #2 
- Switch to iPad screen size in your browser developer tools
- Go to My Account Page Email and submit your email for 2 different addresses
- Go to Submit Page
- Fill out the form with random data and choose Linear curve
- Go to detail page and verify is all displayed data match your submitted data
- From creator's address press Edit button, edit district data randomly, and send transaction
- From detail page, stake and unstake into the district random amounts, randomly many times, from random addresses
- Looking at stake history chart, verify if staking follows chosen curve
- Switch to a different address and challenge the district
- Verify if from creator's address it is not possible to edit district during a challenge
- From challenger's address vote Against with amount larger than currently staked balance
- Click on the link to backup your vote secrets and backup your vote secrets
- Clear localstorage of your browser
- Import your vote secrets file
- Go back to the district detail page
- Reveal your vote
- Verify if the district is blacklisted after reveal period finished
- Verify that the district cannot be edited from creator's address after it was blacklisted
- Verify that challenger's address is egligible for challenge as well as vote reward
- Claim the reward from challenger's address
- Verify if you received email notification about claimed reward
- Go to My Account - Activity and verify is all transactions you submitted are listed there

### User Story #3 
- Switch to a random mobile screen size in your browser developer tools
- Go to My Account Page Email and submit your email for 3 different addresses
- Go to Submit Page
- Fill out the form with random data and choose Exponential curve
- Go to detail page and verify is all displayed data match your submitted data
- From detail page, stake and unstake into the district random amounts, randomly many times, from random addresses
- Looking at stake history chart, verify if staking follows chosen curve
- Unstake all DNT from the district
- Challenge the district
- From challenger's address vote Against with random amount
- Switch to a different address and stake into the district before voting period finishes, with larger amount that you voted Against
- Reval the vote from challenger's address
- Verify if the district is still in registry after reveal period finished
- Verify if the address you staked with last time is egligible for the vote reward and claim the reward
- Verify if creator's address is egligible for the vote reward and claim the reward
- Challenge the district again
- From challenger's address vote Against with larger amount than currently staked balance
- Switch to a different address and vote For with a random amount
- Do not reveal votes from the address you voted For
- Reveal votes from address you voted Against 
- Verify is the district is blacklisted after reveal period finished
- Verify that challenger's address is egligible for challenge as well as vote reward
- Claim the reward
- Verify that voter, who didn't reveal votes, is egligible to reclaim votes back
- Reclaim votes and verify if votes were correctly transferred back
- Go to My Account - Activity and verify is all transactions you submitted are listed there

