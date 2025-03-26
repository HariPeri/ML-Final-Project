import scipy.stats as stats
import pandas as pd
import numpy as np

# load in data frame:
data = pd.read_csv('../../data/train.csv')
df = pd.DataFrame(data)

# remove Id, get features, and get target:
X = df.drop(columns=['Id', 'SalePrice'])
y = df["SalePrice"]

# use scipy library to calculate spearman for all features:
results = []
for x in X.columns:
    rho, _ = stats.spearmanr(X[x], y)
    results.append([x, rho])


# sorting function:
def sorter(item):
    feature, rho = item
    if np.isnan(rho):
        return (0, 0)
    return (1, abs(rho))


# sort results in descending order of correlation:
sorted_results = sorted(results, key=sorter, reverse=True)


# display results:
for result in sorted_results:
    print(f"Feature: {result[0]}, SCC Rho Value: {result[1]}")