import pandas as pd

def determine_quartile(series, bins=4):
    try:
        # do our best w/ qcut to get even spread of the data
        result, bin_edges = pd.qcut(series.fillna(0), q=bins, retbins=True, duplicates='drop')
        labels = [f'Q{i+1}' for i in range(len(bin_edges) - 1)]
        return pd.qcut(series.fillna(0), q=bins, labels=labels, duplicates='drop')
    except ValueError as e:
        # basically qcut wouldn't work due to some entries having too many zeros, so doing a manual quartile calc 
        quantiles = series.fillna(0).quantile([0.25, 0.5, 0.75])
        bins_manual = [-float('inf'), *quantiles, float('inf')]
        labels = ['Q1', 'Q2', 'Q3', 'Q4']
        return pd.cut(series.fillna(0), bins=bins_manual, labels=labels)
    

def categorize_continuous_variables(input_path, output_path):
    df = pd.read_csv(input_path)

    # figure out continuous vars
    continuous_vars = [
        "LotFrontage", "LotArea", "MasVnrArea", "BsmtFinSF1", "BsmtFinSF2", "BsmtUnfSF",
        "TotalBsmtSF", "1stFlrSF", "2ndFlrSF", "LowQualFinSF", "GrLivArea",
        "GarageArea", "WoodDeckSF", "OpenPorchSF", "EnclosedPorch", "3SsnPorch",
        "ScreenPorch", "PoolArea", "MiscVal"
    ]

    # sub in categories
    for var in continuous_vars:
        df[var] = determine_quartile(df[var])

    # store final csv
    df.to_csv(output_path, index=False)


if __name__ == '__main__':
    input_path = 'kaggle_data/train.csv'          
    output_path = 'train_categorized.csv'  
    categorize_continuous_variables(input_path, output_path)