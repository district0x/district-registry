data = read.csv (text =
"supply, price

")

data = read.csv("/home/filip/Downloads/simulation", header = FALSE, col.names = c ("supply", "price"), sep = " ")

plot(x = data$supply, y = data$price, type = "l")

plot(x = 1:length(data$price), y = data$price, type = "l")

sortedData = data[order(data$supply),]

# plot data with fitted linear reg line
plot(x = sortedData$supply, y = sortedData$price, type = "l")

linearModel <- lm(price ~ supply, data = sortedData)
abline(b = linearModel$coefficients[2], a = linearModel$coefficients[1], col = "red")

expModel <- lm(price ~ supply + I(supply^2), data = sortedData)
lines(sortedData$supply, predict(expModel), lwd = 2, col = "blue")
