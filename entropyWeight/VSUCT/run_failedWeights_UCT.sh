trap '' HUP
# Re-run failed weights in previous experiment

jar_file="AgentEval.jar"

# ------------ AGAINST UCT (Clobber Game) ------------

game="Clobber.lud"
game_name="Clobber"
agents="entropyshuctanytime uct"
option=""

# Budget 20000
budget=20000
failed_weights=(0.6 1.0)
for value in "${failed_weights[@]}"; do
    output_folder="${game_name}//budget_${budget}//weight_${value}"
    mkdir -p "$output_folder"
    nohup java -jar $jar_file --game "$game" --game-options $option --agents $agents --out-dir "$output_folder" --anytime-mode true --anytime-budget $budget --entropy-weight $value --num-games 100 --output-alpha-rank-data --output-raw-results > "${output_folder}.out" 2> "${output_folder}.err" &
done

# Budget 50000
budget=50000
failed_weights=(0.3 0.6 0.7)
for value in "${failed_weights[@]}"; do
    output_folder="${game_name}//budget_${budget}//weight_${value}"
    mkdir -p "$output_folder"
    nohup java -jar $jar_file --game "$game" --game-options $option --agents $agents --out-dir "$output_folder" --anytime-mode true --anytime-budget $budget --entropy-weight $value --num-games 100 --output-alpha-rank-data --output-raw-results > "${output_folder}.out" 2> "${output_folder}.err" &
done
