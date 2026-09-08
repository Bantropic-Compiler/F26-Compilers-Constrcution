type Item is record
    var price: integer
end

routine totalPrice() is
    var items: array[3] Item
    items[1].price := 100
    items[2].price := 250
    items[3].price := 75
    var total is 0
    for it in items loop
        total := total + it.price
    end
    print total
end
