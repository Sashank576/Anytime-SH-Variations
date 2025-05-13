trap '' HUP
#Experiment to test the regression tree agent against the standard UCT agent in multiple games with different budgets

#Instantiate parameters for experiment
iteration_budget=(1000 20000 50000)
games=("Clobber.lud" "Breakthrough.lud" "Amazons.lud" "Yavalath.lud")
game_options=("" "" "" "")
agents="regressiontreeshuctany uct"

jar_file="AgentEval.jar"

#Loop through the games and budgets
for i in "${!games[@]}"; 
    do
    game="${games[$i]}"
    option=${game_options[$i]}
    game_name=$(basename "$game" .lud) 

    for budget in "${iteration_budget[@]}"; 
        do
        output_folder="${game_name}//budget_${budget}"

        mkdir -p "$output_folder"

        nohup java -jar $jar_file --game "$game" --game-options $option --agents $agents --out-dir "$output_folder" --anytime-mode true --anytime-budget $budget --thinking-time -1 --iteration-limit $budget --num-games 100 --output-alpha-rank-data --output-raw-results > "${output_folder}.out" 2> "${output_folder}.err" &
    done
done
