trap '' HUP
#Experiment to compute the best value for the entropy weight parameter in the entropy anytime SH agent    

cd //home//i6317806//entropyWeight//BroadSearch

#Instantiate parameters for experiment
iteration_budget=(1000 20000 50000)
entropy_weights=(0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0)
games=("Clobber.lud" "Breakthrough.lud" "Amazons.lud" "Yavalath.lud")
game_options=('"Rows/7" "Columns/7"' "" "" "")
agents="entropyshuctanytime shuctanytime"

jar_file="AgentEval.jar"
output_directory="//home//i6317806//entropyWeight//BroadSearch"

#Loop through the games, budgets and then weights
for i in "${!games[@]}"; 
    do
    game="${games[$i]}"
    option=${game_options[$i]}
    game_name=$(basename "$game" .lud) 

    for budget in "${iteration_budget[@]}"; 
        do
        for value in "${entropy_weights[@]}"; 
            do
            output_folder="${output_directory}/${game_name}/budget_${budget}/weight_${value}"

            mkdir -p "$output_folder"

            nohup java -jar $jar_file --game "$game" --game-options $option --agents $agents --out-dir "$output_folder" --anytime-mode true --anytime-budget $budget --entropy-weight $value --num-games 100 --output-alpha-rank-data --output-raw-results > "${output_folder}.out" 2> "${output_folder}.err" &
        done
    done
done
