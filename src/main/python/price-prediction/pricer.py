import pandas as pd
import numpy as np
import sys
import os
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LinearRegression
#from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor, VotingRegressor
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
import xgboost as xgb

import warnings
warnings.filterwarnings('ignore')

df = pd.read_csv("price-prediction/house_prices.csv")
df = df[['date', 'price', 'bedrooms', 'sqft_living', 'sqft_lot']]

# Adjust prices for inflation
BUYING_POWER_2024 = 1085468.77 # Compared to $100,000 in 1967, per https://www.in2013dollars.com/Housing/price-inflation
BUYING_POWER_2014 = 757570.96
INFLATION_RATIO = (BUYING_POWER_2024 - BUYING_POWER_2014) / BUYING_POWER_2014
df['price'] = (df['price'] + (df['price'] * INFLATION_RATIO)).astype(int)
df = df.drop(columns='date')

# Train-test split
X = df[['bedrooms', 'sqft_living', 'sqft_lot']]
y = df['price']
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

model = LinearRegression()
model.fit(X_train, y_train)
y_pred = model.predict(X_test)
mean_house_price = y_test.mean()
mse = mean_squared_error(y_test, y_pred)
rmse = np.sqrt(mse)
mae = mean_absolute_error(y_test, y_pred)
r2 = r2_score(y_test, y_pred)

'''
print(f"Mean House Price: {mean_house_price:.4f}\n"
      f"MSE:              {mse:.4f}\n" 
      f"RMSE:             {rmse:.4f}\n" 
      f"MAE:              {mae:.4f}\n"
      f"R2:               {r2:.4f}\n")
'''


# Predict price on unseen (input) data
def predict_price(bedrooms, sqft_living, sqft_lot) -> int:
    house_data = np.array([[bedrooms, sqft_living, sqft_lot]])
    price = model.predict(house_data)

    return price[0]


wd = os.getcwd()

input_bedrooms = int(sys.argv[1])
input_sqft_living = int(sys.argv[2])
input_sqft_lot = int(sys.argv[3])
print(predict_price(input_bedrooms, input_sqft_living, input_sqft_lot))
