import pandas as pd
import numpy as np
import sys
import os

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LinearRegression
from sklearn.ensemble import RandomForestRegressor
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.ensemble import VotingRegressor
from sklearn.metrics import r2_score

import warnings
warnings.filterwarnings('ignore')

# Prepare dataframe
df = pd.read_csv("price-prediction/testData.csv")
df = df[['price', 'area', 'bedrooms', 'furnishingstatus']]
df['furnishingstatus'] = df['furnishingstatus'].apply(lambda x: x != 'unfurnished')

# Initialize variables
X = df[['area', 'bedrooms', 'furnishingstatus']]
y = df['price']
scalar = StandardScaler()


def get_vr() -> VotingRegressor:

    # Train-test split
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)
    scalar.fit(X_train)
    X_train = scalar.transform(X_train)
    X_test = scalar.transform(X_test)

    # Initialize regression techniques
    lr = LinearRegression()
    gb = GradientBoostingRegressor()
    rf = RandomForestRegressor()

    regressors = [("lr", lr), ("gb", gb), ("rf", rf)]
    '''
    for regressor in regressors:
        scores = cross_val_score(estimator[1], X_train, y_train, scoring='r2', cv=10)
        print(regressor[0], np.round(np.mean(scores), 2))
    '''

    vr = VotingRegressor(regressors)
    vr.fit(X_train, y_train)
    y_pred = vr.predict(X_test)
    r2 = r2_score(y_test, y_pred)

    if r2 < 0.5:
        get_vr()

    return vr


def predict_price(area, bedrooms, furnishingstatus) -> int:
    house_data = np.array([[area, bedrooms, furnishingstatus]])
    house_data = scalar.transform(house_data)
    price = vr.predict(house_data)

    return price[0]


wd = os.getcwd()

vr = get_vr()
inputArea = int(sys.argv[1])
inputBedrooms = int(sys.argv[2])
inputFurnishingstatus = bool(sys.argv[3])
print(predict_price(inputArea, inputBedrooms, inputFurnishingstatus))
