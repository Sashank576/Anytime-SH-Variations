trap '' HUP
#Experiment to compute the best exploration constant used in the UCB1 function for both agents    

#Instantiate parameters for experiment
iteration_budget=(1000 20000 50000)
exploration_values=(1.0 1.15 1.3 1.45 1.6 1.75 1.9)
games=("Clobber.lud" "Breakthrough.lud" "Amazons.lud" "Yavalath.lud")
game_options=("" "" "" "")
agents="doubleiterregressiontreeshuctany uct"

jar_file="AgentEval.jar"

#Loop through the games, budgets and then values
for i in "${!games[@]}"; 
    do
    game="${games[$i]}"
    option=${game_options[$i]}
    game_name=$(basename "$game" .lud) 

    for budget in "${iteration_budget[@]}"; 
        do
        for value in "${exploration_values[@]}"; 
            do
            output_folder="${game_name}//budget_${budget}//value_${value}"

            mkdir -p "$output_folder"

            nohup java -jar $jar_file --game "$game" --game-options $option --agents $agents --out-dir "$output_folder" --anytime-mode true --anytime-budget $budget --exploration-constant $value --thinking-time -1 --iteration-limit $budget --num-games 150 --output-alpha-rank-data --output-raw-results > "${output_folder}.out" 2> "${output_folder}.err" &
        done
    done
done
