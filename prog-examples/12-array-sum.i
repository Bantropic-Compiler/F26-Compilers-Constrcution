routine sumArray() is
    var values: array[5] integer
    values[1] := 10
    values[2] := 20
    values[3] := 30
    values[4] := 40
    values[5] := 50
    var total is 0
    for v in values loop
        total := total + v
    end
    print total
end
