trap '' HUP
# Re-run failed exploration constants for regressiontreeshuctany agent

jar_file="AgentEval.jar"
game="Clobber.lud"
game_name="Clobber"
agents="regressiontreeshuctany shuctanytime"
option=""

# Budget 20000
budget=20000
failed_constants=(1.3)
for value in "${failed_constants[@]}"; do
    output_folder="${game_name}//budget_${budget}//value_${value}"
    mkdir -p "$output_folder"
    nohup java -jar $jar_file --game "$game" --game-options $option --agents $agents --out-dir "$output_folder" --anytime-mode true --anytime-budget $budget --exploration-constant $value --thinking-time -1 --iteration-limit $budget --num-games 100 --output-alpha-rank-data --output-raw-results > "${output_folder}.out" 2> "${output_folder}.err" &
done

# Budget 50000
budget=50000
failed_constants=(1.75 1.45 1.9 1.6 1.3)
for value in "${failed_constants[@]}"; do
    output_folder="${game_name}//budget_${budget}//value_${value}"
    mkdir -p "$output_folder"
    nohup java -jar $jar_file --game "$game" --game-options $option --agents $agents --out-dir "$output_folder" --anytime-mode true --anytime-budget $budget --exploration-constant $value --thinking-time -1 --iteration-limit $budget --num-games 100 --output-alpha-rank-data --output-raw-results > "${output_folder}.out" 2> "${output_folder}.err" &
done
