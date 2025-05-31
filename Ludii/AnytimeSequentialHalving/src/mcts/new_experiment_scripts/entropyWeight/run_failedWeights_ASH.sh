trap '' HUP
# Re-run failed weights in previous experiment

jar_file="AgentEval.jar"

game="Amazons.lud"
game_name="Amazons"
agents="entropyshuctanytime shuctanytime"
option=""
budget=50000
failed_weights=(0.7 0.9 1.0 0.8 0.3 0.1 0.6 0.2)

for value in "${failed_weights[@]}"; do
    output_folder="${game_name}//budget_${budget}//weight_${value}"
    mkdir -p "$output_folder"
    nohup java -jar $jar_file --game "$game" --game-options $option --agents $agents --out-dir "$output_folder" --anytime-mode true --anytime-budget $budget --entropy-weight $value --num-games 100 --output-alpha-rank-data --output-raw-results > "${output_folder}.out" 2> "${output_folder}.err" &
done

